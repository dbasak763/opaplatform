import json
import logging
from kafka import KafkaConsumer
from typing import Dict, Any, Callable
from datetime import datetime, timezone
import copy
from collections import deque

logger = logging.getLogger(__name__)

PROCESSED_EVENT_TTL_SECONDS = 24 * 60 * 60
MAX_MINUTE_BUCKETS = 120
MAX_HOUR_BUCKETS = 48
MAX_RECENT_EVENTS = 5000

class OrderEventConsumer:
    def __init__(self, bootstrap_servers: str = "localhost:9092"):
        self.bootstrap_servers = bootstrap_servers
        self.consumer = None
        self.running = False
        self.event_handlers: Dict[str, Callable] = {}
        
    def register_handler(self, event_type: str, handler: Callable):
        """Register a handler for a specific event type"""
        self.event_handlers[event_type] = handler
        
    def start_consumer(self, topics: list[str]):
        """Start consuming messages from Kafka topics"""
        try:
            self.consumer = KafkaConsumer(
                *topics,
                bootstrap_servers=self.bootstrap_servers,
                value_deserializer=lambda x: json.loads(x.decode('utf-8')),
                group_id='analytics-service',
                auto_offset_reset='latest',
                enable_auto_commit=True
            )
            
            self.running = True
            logger.info(f"Started consuming from topics: {topics}")
            
            for message in self.consumer:
                if not self.running:
                    break
                    
                try:
                    event_data = message.value
                    event_type = event_data.get('eventType', 'unknown')
                    
                    logger.info(f"Received event: {event_type} for order: {event_data.get('orderId')}")
                    
                    # Process event with registered handler
                    if event_type in self.event_handlers:
                        self.event_handlers[event_type](event_data)
                    else:
                        logger.warning(f"No handler registered for event type: {event_type}")
                        
                except Exception as e:
                    logger.error(f"Error processing message: {e}")
                    
        except Exception as e:
            logger.error(f"Error starting Kafka consumer: {e}")
            
    def stop_consumer(self):
        """Stop the Kafka consumer"""
        self.running = False
        if self.consumer:
            self.consumer.close()
            logger.info("Kafka consumer stopped")

class AnalyticsProcessor:
    def __init__(self, cassandra_session=None, redis_client=None):
        self.cassandra_session = cassandra_session
        self.redis_client = redis_client
        self.processed_event_cache = deque()
        self.processed_event_lookup = set()
        self.metrics_cache = self._default_metrics()
        self.initialize_metrics_from_store()

    def process_order_created(self, event_data: Dict[str, Any]):
        """Process order created events"""
        try:
            if not self._should_process_event(event_data):
                return

            order_id = event_data.get('orderId')
            total_amount = float(event_data.get('totalAmount', 0) or 0)
            timestamp = self._parse_timestamp(event_data.get('timestamp'))
            status = event_data.get('status') or event_data.get('orderStatus') or 'PENDING'

            self._update_order_metrics(total_amount, timestamp)
            self._increment_status(status)

            if self.cassandra_session:
                self._store_order_event(event_data)

            if self.redis_client:
                self._update_realtime_cache(event_data)

            self._persist_metrics()
            logger.info(f"Processed order created event for order: {order_id}")

        except Exception as e:
            logger.error(f"Error processing order created event: {e}")

    def process_order_status_changed(self, event_data: Dict[str, Any]):
        """Process order status change events"""
        try:
            if not self._should_process_event(event_data):
                return

            old_status = event_data.get('oldStatus') or event_data.get('previousStatus')
            new_status = event_data.get('newStatus') or event_data.get('status')

            self._update_status_metrics(old_status, new_status)

            if self.cassandra_session:
                self._store_status_change(event_data)

            self._persist_metrics()
            logger.info(
                "Processed status change for order %s: %s -> %s",
                event_data.get('orderId'),
                old_status,
                new_status
            )

        except Exception as e:
            logger.error(f"Error processing order status change: {e}")

    def process_order_cancelled(self, event_data: Dict[str, Any]):
        """Process order cancellation events"""
        try:
            if not self._should_process_event(event_data):
                return

            previous_status = event_data.get('previousStatus') or event_data.get('oldStatus')
            self._update_status_metrics(previous_status, 'CANCELLED')
            self._increment_cancellations()

            if self.cassandra_session:
                self._store_status_change(event_data)

            self._persist_metrics()
            logger.info(
                "Processed order cancellation for order: %s",
                event_data.get('orderId')
            )

        except Exception as e:
            logger.error(f"Error processing order cancellation: {e}")

    # -------------------- Metrics Helpers --------------------

    def _default_metrics(self) -> Dict[str, Any]:
        return {
            'total_orders': 0,
            'total_revenue': 0.0,
            'avg_order_value': 0.0,
            'cancelled_orders': 0,
            'orders_by_status': {},
            'orders_per_hour': {},
            'orders_per_minute': {},
            'revenue_per_hour': {},
            'revenue_per_minute': {}
        }

    def _parse_timestamp(self, raw_value) -> datetime:
        if isinstance(raw_value, list):
            # Handle [year, month, day, hour, minute, second, nanos]
            year, month, day, hour, minute, second, nanos = (raw_value + [0] * 7)[:7]
            microseconds = int((nanos or 0) / 1000)
            return datetime(year, month, day, hour, minute, second, microseconds, tzinfo=timezone.utc)

        if isinstance(raw_value, (int, float)):
            return datetime.fromtimestamp(raw_value, tz=timezone.utc)

        if isinstance(raw_value, str) and raw_value:
            value = raw_value.strip()
            if value.endswith('Z'):
                value = value[:-1] + '+00:00'
            try:
                return datetime.fromisoformat(value)
            except ValueError:
                logger.warning("Unable to parse timestamp '%s', defaulting to now", raw_value)

        return datetime.utcnow().replace(tzinfo=timezone.utc)

    def _should_process_event(self, event_data: Dict[str, Any]) -> bool:
        event_id = event_data.get('eventId') or event_data.get('id')
        if not event_id:
            return True

        event_id = str(event_id)

        if self.redis_client:
            key = f"analytics:event:{event_id}"
            created = self.redis_client.set(key, 1, ex=PROCESSED_EVENT_TTL_SECONDS, nx=True)
            if created:
                return True
            return False

        if event_id in self.processed_event_lookup:
            return False

        self.processed_event_cache.append(event_id)
        self.processed_event_lookup.add(event_id)
        if len(self.processed_event_cache) > MAX_RECENT_EVENTS:
            oldest = self.processed_event_cache.popleft()
            self.processed_event_lookup.discard(oldest)
        
        return True

    def _update_order_metrics(self, amount: float, timestamp: datetime):
        minute_key = timestamp.strftime('%Y-%m-%d-%H-%M')
        hour_key = timestamp.strftime('%Y-%m-%d-%H')

        self._increment_bucket('orders_per_minute', minute_key, 1, MAX_MINUTE_BUCKETS)
        self._increment_bucket('revenue_per_minute', minute_key, amount, MAX_MINUTE_BUCKETS)
        self._increment_bucket('orders_per_hour', hour_key, 1, MAX_HOUR_BUCKETS)
        self._increment_bucket('revenue_per_hour', hour_key, amount, MAX_HOUR_BUCKETS)

        self.metrics_cache['total_orders'] += 1
        self.metrics_cache['total_revenue'] += amount

        if self.metrics_cache['total_orders'] > 0:
            self.metrics_cache['avg_order_value'] = (
                self.metrics_cache['total_revenue'] / self.metrics_cache['total_orders']
            )

    def _increment_bucket(self, cache_key: str, bucket_key: str, value: float, limit: int):
        bucket = self.metrics_cache.setdefault(cache_key, {})
        bucket[bucket_key] = float(bucket.get(bucket_key, 0)) + value

        if len(bucket) > limit:
            oldest_key = min(bucket.keys())
            bucket.pop(oldest_key, None)

    def _increment_status(self, status: str, delta: int = 1):
        if not status:
            return
        status_map = self.metrics_cache.setdefault('orders_by_status', {})
        status_map[status] = max(0, status_map.get(status, 0) + delta)

    def _update_status_metrics(self, old_status: str, new_status: str):
        if old_status and old_status != new_status:
            self._increment_status(old_status, -1)
        if new_status:
            self._increment_status(new_status, 1)

    def _increment_cancellations(self):
        self.metrics_cache['cancelled_orders'] = self.metrics_cache.get('cancelled_orders', 0) + 1

    # -------------------- Persistence Helpers --------------------

    def get_current_metrics(self) -> Dict[str, Any]:
        """Get current metrics from cache"""
        return copy.deepcopy(self.metrics_cache)

    def initialize_metrics_from_store(self):
        """Load persisted metrics from Redis if available."""
        if not self.redis_client:
            return

        try:
            data = self.redis_client.hgetall("analytics:metrics")
            if not data:
                return

            metrics = self._default_metrics()
            metrics['total_orders'] = int(float(data.get('total_orders', 0) or 0))
            metrics['total_revenue'] = float(data.get('total_revenue', 0.0) or 0.0)
            metrics['cancelled_orders'] = int(float(data.get('cancelled_orders', 0) or 0))
            metrics['avg_order_value'] = float(data.get('avg_order_value', 0.0) or 0.0)

            metrics['orders_by_status'] = json.loads(data.get('orders_by_status', '{}') or '{}')
            metrics['orders_per_hour'] = json.loads(data.get('orders_per_hour', '{}') or '{}')
            metrics['orders_per_minute'] = json.loads(data.get('orders_per_minute', '{}') or '{}')
            metrics['revenue_per_hour'] = json.loads(data.get('revenue_per_hour', '{}') or '{}')
            metrics['revenue_per_minute'] = json.loads(data.get('revenue_per_minute', '{}') or '{}')

            self.metrics_cache = metrics

        except Exception as exc:
            logger.error(f"Error loading metrics from Redis: {exc}")

    def _persist_metrics(self):
        """Persist current metrics to Redis for durability."""
        if not self.redis_client:
            return

        try:
            self.redis_client.hset(
                "analytics:metrics",
                mapping={
                    "total_orders": self.metrics_cache.get('total_orders', 0),
                    "total_revenue": self.metrics_cache.get('total_revenue', 0.0),
                    "avg_order_value": self.metrics_cache.get('avg_order_value', 0.0),
                    "cancelled_orders": self.metrics_cache.get('cancelled_orders', 0),
                    "orders_by_status": json.dumps(self.metrics_cache.get('orders_by_status', {})),
                    "orders_per_hour": json.dumps(self.metrics_cache.get('orders_per_hour', {})),
                    "orders_per_minute": json.dumps(self.metrics_cache.get('orders_per_minute', {})),
                    "revenue_per_hour": json.dumps(self.metrics_cache.get('revenue_per_hour', {})),
                    "revenue_per_minute": json.dumps(self.metrics_cache.get('revenue_per_minute', {}))
                }
            )
        except Exception as exc:
            logger.error(f"Error persisting metrics to Redis: {exc}")

    # -------------------- Storage Hooks --------------------

    def _store_order_event(self, event_data: Dict[str, Any]):
        """Store order event in Cassandra (placeholder)."""
        pass

    def _store_status_change(self, event_data: Dict[str, Any]):
        """Store order status change in Cassandra (placeholder)."""
        pass

    def _update_realtime_cache(self, event_data: Dict[str, Any]):
        """Update Redis cache for real-time metrics"""
        if not self.redis_client:
            return

        try:
            event_copy = copy.deepcopy(event_data)
            event_copy['timestamp'] = self._parse_timestamp(event_copy.get('timestamp')).isoformat()

            recent_orders_key = "recent_orders"
            self.redis_client.lpush(recent_orders_key, json.dumps(event_copy))
            self.redis_client.ltrim(recent_orders_key, 0, 99)  # Keep last 100 orders

            self.redis_client.incr("total_orders_today")
            self.redis_client.incrbyfloat("revenue_today", event_data.get('totalAmount', 0))

        except Exception as e:
            logger.error(f"Error updating Redis cache: {e}")
