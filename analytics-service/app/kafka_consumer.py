import copy
import json
import logging
import os
import time
import uuid
from collections import deque
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Callable, Dict

from kafka import KafkaConsumer

logger = logging.getLogger(__name__)

PROCESSED_EVENT_TTL_SECONDS = 24 * 60 * 60
MAX_MINUTE_BUCKETS = 120
MAX_HOUR_BUCKETS = 48
MAX_RECENT_EVENTS = 5000


class OrderEventConsumer:
    def __init__(self, bootstrap_servers: str | None = None):
        self.bootstrap_servers = bootstrap_servers or os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        self.group_id = os.getenv("KAFKA_CONSUMER_GROUP", "analytics-service")
        self.consumer = None
        self.running = False
        self.ready = False
        self.event_handlers: Dict[str, Callable] = {}

    def register_handler(self, event_type: str, handler: Callable):
        self.event_handlers[event_type] = handler

    def start_consumer(self, topics: list[str]):
        self.running = True
        while self.running:
            try:
                self.consumer = KafkaConsumer(
                    *topics,
                    bootstrap_servers=self.bootstrap_servers,
                    value_deserializer=lambda value: json.loads(value.decode("utf-8")),
                    group_id=self.group_id,
                    auto_offset_reset="earliest",
                    enable_auto_commit=False,
                )
                self.ready = True
                logger.info("Consuming %s from %s", topics, self.bootstrap_servers)
                for message in self.consumer:
                    if not self.running:
                        break
                    event = message.value
                    event_type = event.get("eventType", "UNKNOWN")
                    handler = self.event_handlers.get(event_type)
                    if not handler:
                        logger.warning("Ignoring unsupported event type %s", event_type)
                        self.consumer.commit()
                        continue
                    try:
                        handler(event)
                        self.consumer.commit()
                    except Exception:
                        logger.exception("Failed to process event %s; offset was not committed", event.get("eventId"))
                        time.sleep(1)
            except Exception:
                self.ready = False
                if self.running:
                    logger.exception("Kafka consumer disconnected; retrying in 5 seconds")
                    time.sleep(5)
            finally:
                if self.consumer:
                    self.consumer.close()
                    self.consumer = None
        self.ready = False

    def stop_consumer(self):
        self.running = False
        if self.consumer:
            self.consumer.close()


class AnalyticsProcessor:
    def __init__(self, cassandra_session=None, redis_client=None):
        self.cassandra_session = cassandra_session
        self.redis_client = redis_client
        self.processed_event_cache = deque()
        self.processed_event_lookup = set()
        self.metrics_cache = self._default_metrics()

    def process_order_created(self, event: Dict[str, Any]):
        if not self._should_process_event(event):
            return
        amount = float(event.get("totalAmount", 0) or 0)
        timestamp = self._parse_timestamp(event.get("timestamp"))
        status = event.get("status") or "PENDING"
        self._update_order_metrics(amount, timestamp)
        self._increment_status(status)
        self._update_product_metrics(event.get("items") or [])
        user_id = str(event.get("userId") or "")
        if user_id and user_id not in self.metrics_cache["active_user_ids"]:
            self.metrics_cache["active_user_ids"].append(user_id)
        self._store_order_event(event)
        self._update_realtime_cache(event)
        self._persist_metrics()
        self._mark_processed(event)

    def process_order_status_changed(self, event: Dict[str, Any]):
        if not self._should_process_event(event):
            return
        self._update_status_metrics(event.get("previousStatus"), event.get("newStatus"))
        self._store_order_event(event)
        self._update_realtime_cache(event)
        self._persist_metrics()
        self._mark_processed(event)

    def process_order_cancelled(self, event: Dict[str, Any]):
        if not self._should_process_event(event):
            return
        self._update_status_metrics(event.get("previousStatus"), "CANCELLED")
        self.metrics_cache["cancelled_orders"] += 1
        refund = float(event.get("refundAmount", 0) or 0)
        self.metrics_cache["total_revenue"] = max(0.0, self.metrics_cache["total_revenue"] - refund)
        self._recalculate_average()
        self._store_order_event(event)
        self._update_realtime_cache(event)
        self._persist_metrics()
        self._mark_processed(event)

    def _default_metrics(self) -> Dict[str, Any]:
        return {
            "total_orders": 0,
            "total_revenue": 0.0,
            "avg_order_value": 0.0,
            "cancelled_orders": 0,
            "orders_by_status": {},
            "orders_per_hour": {},
            "orders_per_minute": {},
            "revenue_per_hour": {},
            "revenue_per_minute": {},
            "product_metrics": {},
            "active_user_ids": [],
        }

    def _parse_timestamp(self, raw_value) -> datetime:
        if isinstance(raw_value, list):
            year, month, day, hour, minute, second, nanos = (raw_value + [0] * 7)[:7]
            return datetime(year, month, day, hour, minute, second, int((nanos or 0) / 1000), tzinfo=timezone.utc)
        if isinstance(raw_value, (int, float)):
            return datetime.fromtimestamp(raw_value, tz=timezone.utc)
        if isinstance(raw_value, str) and raw_value:
            value = raw_value.strip().replace("Z", "+00:00")
            try:
                parsed = datetime.fromisoformat(value)
                return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
            except ValueError:
                logger.warning("Invalid event timestamp %s", raw_value)
        return datetime.now(timezone.utc)

    def _event_id(self, event: Dict[str, Any]) -> str:
        return str(event.get("eventId") or event.get("id") or "")

    def _should_process_event(self, event: Dict[str, Any]) -> bool:
        event_id = self._event_id(event)
        if not event_id:
            return True
        if self.redis_client and self.redis_client.exists(f"analytics:event:{event_id}"):
            return False
        return event_id not in self.processed_event_lookup

    def _mark_processed(self, event: Dict[str, Any]):
        event_id = self._event_id(event)
        if not event_id:
            return
        if self.redis_client:
            self.redis_client.set(f"analytics:event:{event_id}", 1, ex=PROCESSED_EVENT_TTL_SECONDS)
        self.processed_event_cache.append(event_id)
        self.processed_event_lookup.add(event_id)
        if len(self.processed_event_cache) > MAX_RECENT_EVENTS:
            self.processed_event_lookup.discard(self.processed_event_cache.popleft())

    def _update_order_metrics(self, amount: float, timestamp: datetime):
        minute_key = timestamp.strftime("%Y-%m-%d-%H-%M")
        hour_key = timestamp.strftime("%Y-%m-%d-%H")
        self._increment_bucket("orders_per_minute", minute_key, 1, MAX_MINUTE_BUCKETS)
        self._increment_bucket("revenue_per_minute", minute_key, amount, MAX_MINUTE_BUCKETS)
        self._increment_bucket("orders_per_hour", hour_key, 1, MAX_HOUR_BUCKETS)
        self._increment_bucket("revenue_per_hour", hour_key, amount, MAX_HOUR_BUCKETS)
        self.metrics_cache["total_orders"] += 1
        self.metrics_cache["total_revenue"] += amount
        self._recalculate_average()

    def _recalculate_average(self):
        total = self.metrics_cache["total_orders"]
        self.metrics_cache["avg_order_value"] = self.metrics_cache["total_revenue"] / total if total else 0.0

    def _increment_bucket(self, cache_key: str, bucket_key: str, value: float, limit: int):
        bucket = self.metrics_cache[cache_key]
        bucket[bucket_key] = float(bucket.get(bucket_key, 0)) + value
        while len(bucket) > limit:
            bucket.pop(min(bucket), None)

    def _increment_status(self, status: str | None, delta: int = 1):
        if status:
            statuses = self.metrics_cache["orders_by_status"]
            statuses[status] = max(0, int(statuses.get(status, 0)) + delta)

    def _update_status_metrics(self, old_status: str | None, new_status: str | None):
        if old_status and old_status != new_status:
            self._increment_status(old_status, -1)
        if new_status:
            self._increment_status(new_status, 1)

    def _update_product_metrics(self, items: list[Dict[str, Any]]):
        products = self.metrics_cache["product_metrics"]
        for item in items:
            product_id = str(item.get("productId") or item.get("productName") or "unknown")
            metric = products.setdefault(
                product_id,
                {
                    "product_id": product_id,
                    "product_name": item.get("productName") or "Unknown product",
                    "total_quantity_sold": 0,
                    "total_revenue": 0.0,
                    "order_count": 0,
                },
            )
            metric["total_quantity_sold"] += int(item.get("quantity", 0) or 0)
            metric["total_revenue"] += float(item.get("totalPrice", 0) or 0)
            metric["order_count"] += 1

    def get_current_metrics(self) -> Dict[str, Any]:
        return copy.deepcopy(self.metrics_cache)

    def get_top_products(self, limit: int = 10) -> list[Dict[str, Any]]:
        values = list(self.metrics_cache["product_metrics"].values())
        return sorted(values, key=lambda item: (item["total_revenue"], item["total_quantity_sold"]), reverse=True)[:limit]

    def initialize_metrics_from_store(self):
        payload = None
        if self.redis_client:
            payload = self.redis_client.get("analytics:metrics:snapshot")
        if not payload and self.cassandra_session:
            row = self.cassandra_session.execute(
                "SELECT payload FROM analytics_metrics WHERE metric_key = %s", ("current",)
            ).one()
            payload = row.payload if row else None
        if payload:
            stored = json.loads(payload)
            defaults = self._default_metrics()
            defaults.update(stored)
            self.metrics_cache = defaults

    def _persist_metrics(self):
        payload = json.dumps(self.metrics_cache)
        now = datetime.now(timezone.utc)
        if self.redis_client:
            self.redis_client.set("analytics:metrics:snapshot", payload)
        if not self.cassandra_session:
            raise RuntimeError("Cassandra is required for durable analytics metrics")
        self.cassandra_session.execute(
            "INSERT INTO analytics_metrics (metric_key, payload, updated_at) VALUES (%s, %s, %s)",
            ("current", payload, now),
        )
        for hour, order_count in self.metrics_cache["orders_per_hour"].items():
            revenue = float(self.metrics_cache["revenue_per_hour"].get(hour, 0.0))
            average = revenue / order_count if order_count else 0.0
            self.cassandra_session.execute(
                """
                INSERT INTO order_metrics_hourly
                (date_hour, order_count, total_revenue, avg_order_value, updated_at)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (hour, int(order_count), Decimal(str(revenue)), Decimal(str(average)), now),
            )

    def _store_order_event(self, event: Dict[str, Any]):
        if not self.cassandra_session:
            raise RuntimeError("Cassandra is required for event persistence")
        raw_event_id = self._event_id(event)
        event_id = uuid.UUID(raw_event_id) if raw_event_id else uuid.uuid4()
        self.cassandra_session.execute(
            """
            INSERT INTO order_events (event_id, event_type, order_id, user_id, timestamp, data)
            VALUES (%s, %s, %s, %s, %s, %s)
            """,
            (
                event_id,
                event.get("eventType", "UNKNOWN"),
                str(event.get("orderId") or ""),
                str(event.get("userId") or ""),
                self._parse_timestamp(event.get("timestamp")),
                json.dumps(event),
            ),
        )

    def _update_realtime_cache(self, event: Dict[str, Any]):
        if not self.redis_client:
            return
        event_copy = copy.deepcopy(event)
        event_copy["timestamp"] = self._parse_timestamp(event_copy.get("timestamp")).isoformat()
        self.redis_client.lpush("recent_orders", json.dumps(event_copy))
        self.redis_client.ltrim("recent_orders", 0, 99)
