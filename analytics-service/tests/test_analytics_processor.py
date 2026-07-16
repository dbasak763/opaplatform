import json
import uuid

from app.kafka_consumer import AnalyticsProcessor


class FakeResult:
    def one(self):
        return None


class FakeCassandra:
    def __init__(self):
        self.calls = []

    def execute(self, query, params=None):
        self.calls.append((" ".join(query.split()), params))
        return FakeResult()


class FakeRedis:
    def __init__(self):
        self.values = {}
        self.lists = {}

    def exists(self, key):
        return key in self.values

    def set(self, key, value, **kwargs):
        self.values[key] = value
        return True

    def get(self, key):
        return self.values.get(key)

    def lpush(self, key, value):
        self.lists.setdefault(key, []).insert(0, value)

    def ltrim(self, key, start, end):
        self.lists[key] = self.lists.get(key, [])[start : end + 1]


def created_event():
    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "ORDER_CREATED",
        "orderId": str(uuid.uuid4()),
        "userId": str(uuid.uuid4()),
        "timestamp": "2025-10-30T03:27:17Z",
        "totalAmount": 119.98,
        "status": "PENDING",
        "items": [
            {
                "productId": "660e8400-e29b-41d4-a716-446655440001",
                "productName": "Wireless Bluetooth Headphones",
                "quantity": 1,
                "unitPrice": 99.99,
                "totalPrice": 99.99,
            }
        ],
    }


def test_created_order_updates_and_persists_metrics_once():
    redis = FakeRedis()
    cassandra = FakeCassandra()
    processor = AnalyticsProcessor(cassandra_session=cassandra, redis_client=redis)
    event = created_event()

    processor.process_order_created(event)
    processor.process_order_created(event)

    metrics = processor.get_current_metrics()
    assert metrics["total_orders"] == 1
    assert metrics["total_revenue"] == 119.98
    assert metrics["orders_by_status"] == {"PENDING": 1}
    assert len(metrics["active_user_ids"]) == 1
    product = processor.get_top_products(1)[0]
    assert product["product_name"] == "Wireless Bluetooth Headphones"
    assert product["total_quantity_sold"] == 1
    assert json.loads(redis.values["analytics:metrics:snapshot"])["total_orders"] == 1
    assert len(redis.lists["recent_orders"]) == 1
    assert any("INSERT INTO order_events" in query for query, _ in cassandra.calls)
    assert any("INSERT INTO analytics_metrics" in query for query, _ in cassandra.calls)


def test_status_and_cancellation_metrics_are_persisted():
    redis = FakeRedis()
    cassandra = FakeCassandra()
    processor = AnalyticsProcessor(cassandra_session=cassandra, redis_client=redis)
    event = created_event()
    processor.process_order_created(event)

    processor.process_order_status_changed(
        {
            **event,
            "eventId": str(uuid.uuid4()),
            "eventType": "ORDER_STATUS_CHANGED",
            "previousStatus": "PENDING",
            "newStatus": "CONFIRMED",
        }
    )
    processor.process_order_cancelled(
        {
            **event,
            "eventId": str(uuid.uuid4()),
            "eventType": "ORDER_CANCELLED",
            "previousStatus": "CONFIRMED",
            "refundAmount": 119.98,
        }
    )

    metrics = processor.get_current_metrics()
    assert metrics["orders_by_status"]["PENDING"] == 0
    assert metrics["orders_by_status"]["CONFIRMED"] == 0
    assert metrics["orders_by_status"]["CANCELLED"] == 1
    assert metrics["cancelled_orders"] == 1
    assert metrics["total_revenue"] == 0.0
