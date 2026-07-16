import logging
import os
from typing import Optional

import redis
from cassandra.cluster import Cluster

logger = logging.getLogger(__name__)


class DatabaseConnections:
    def __init__(self):
        self.redis_client: Optional[redis.Redis] = None
        self.cassandra_session = None
        self.cassandra_cluster = None

    def connect_redis(self):
        host = os.getenv("REDIS_HOST", "localhost")
        port = int(os.getenv("REDIS_PORT", "6379"))
        db = int(os.getenv("REDIS_DB", "0"))
        try:
            client = redis.Redis(
                host=host,
                port=port,
                db=db,
                decode_responses=True,
                socket_connect_timeout=5,
                socket_timeout=5,
            )
            client.ping()
            self.redis_client = client
            logger.info("Connected to Redis at %s:%s", host, port)
            return client
        except Exception as exc:
            logger.warning("Redis connection failed: %s", exc)
            self.redis_client = None
            return None

    def connect_cassandra(self):
        hosts = [host.strip() for host in os.getenv("CASSANDRA_HOSTS", "localhost").split(",") if host.strip()]
        port = int(os.getenv("CASSANDRA_PORT", "9042"))
        keyspace = os.getenv("CASSANDRA_KEYSPACE", "analytics")
        try:
            cluster = Cluster(hosts, port=port)
            session = cluster.connect()
            session.execute(
                f"""
                CREATE KEYSPACE IF NOT EXISTS {keyspace}
                WITH REPLICATION = {{'class': 'SimpleStrategy', 'replication_factor': 1}}
                """
            )
            session.set_keyspace(keyspace)
            self.cassandra_cluster = cluster
            self.cassandra_session = session
            self._create_cassandra_tables()
            logger.info("Connected to Cassandra keyspace %s", keyspace)
            return session
        except Exception as exc:
            logger.warning("Cassandra connection failed: %s", exc)
            if self.cassandra_cluster:
                self.cassandra_cluster.shutdown()
            self.cassandra_cluster = None
            self.cassandra_session = None
            return None

    def _create_cassandra_tables(self):
        statements = [
            """
            CREATE TABLE IF NOT EXISTS order_events (
                event_id UUID PRIMARY KEY,
                event_type TEXT,
                order_id TEXT,
                user_id TEXT,
                timestamp TIMESTAMP,
                data TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS analytics_metrics (
                metric_key TEXT PRIMARY KEY,
                payload TEXT,
                updated_at TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS order_metrics_hourly (
                date_hour TEXT PRIMARY KEY,
                order_count BIGINT,
                total_revenue DECIMAL,
                avg_order_value DECIMAL,
                updated_at TIMESTAMP
            )
            """,
        ]
        for statement in statements:
            self.cassandra_session.execute(statement)

    def close_connections(self):
        if self.redis_client:
            self.redis_client.close()
            self.redis_client = None
        if self.cassandra_cluster:
            self.cassandra_cluster.shutdown()
            self.cassandra_cluster = None
            self.cassandra_session = None
