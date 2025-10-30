import redis
from cassandra.cluster import Cluster
from cassandra.auth import PlainTextAuthProvider
import logging
from typing import Optional

logger = logging.getLogger(__name__)

class DatabaseConnections:
    def __init__(self):
        self.redis_client: Optional[redis.Redis] = None
        self.cassandra_session = None
        self.cassandra_cluster = None
        
    def connect_redis(self, host: str = "localhost", port: int = 6379, db: int = 0):
        """Connect to Redis"""
        try:
            self.redis_client = redis.Redis(
                host=host,
                port=port,
                db=db,
                decode_responses=True,
                socket_connect_timeout=5,
                socket_timeout=5
            )
            # Test connection
            self.redis_client.ping()
            logger.info("Connected to Redis successfully")
            return self.redis_client
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
            return None
            
    def connect_cassandra(self, hosts: list = None, keyspace: str = "analytics"):
        """Connect to Cassandra"""
        if hosts is None:
            hosts = ["localhost"]
            
        try:
            # Create cluster connection
            self.cassandra_cluster = Cluster(hosts)
            self.cassandra_session = self.cassandra_cluster.connect()
            
            # Create keyspace if it doesn't exist
            self.cassandra_session.execute(f"""
                CREATE KEYSPACE IF NOT EXISTS {keyspace}
                WITH REPLICATION = {{
                    'class': 'SimpleStrategy',
                    'replication_factor': 1
                }}
            """)
            
            # Use the keyspace
            self.cassandra_session.set_keyspace(keyspace)
            
            # Create tables
            self._create_cassandra_tables()
            
            logger.info(f"Connected to Cassandra keyspace: {keyspace}")
            return self.cassandra_session
            
        except Exception as e:
            logger.error(f"Failed to connect to Cassandra: {e}")
            return None
            
    def _create_cassandra_tables(self):
        """Create necessary Cassandra tables"""
        try:
            # Order events table
            self.cassandra_session.execute("""
                CREATE TABLE IF NOT EXISTS order_events (
                    event_id UUID PRIMARY KEY,
                    event_type TEXT,
                    order_id TEXT,
                    user_id TEXT,
                    timestamp TIMESTAMP,
                    data TEXT
                )
            """)
            
            # Order metrics by hour
            self.cassandra_session.execute("""
                CREATE TABLE IF NOT EXISTS order_metrics_hourly (
                    date_hour TEXT PRIMARY KEY,
                    order_count INT,
                    total_revenue DECIMAL,
                    avg_order_value DECIMAL,
                    updated_at TIMESTAMP
                )
            """)
            
            # Product metrics
            self.cassandra_session.execute("""
                CREATE TABLE IF NOT EXISTS product_metrics (
                    product_id TEXT PRIMARY KEY,
                    product_name TEXT,
                    total_quantity_sold INT,
                    total_revenue DECIMAL,
                    order_count INT,
                    updated_at TIMESTAMP
                )
            """)
            
            # User metrics
            self.cassandra_session.execute("""
                CREATE TABLE IF NOT EXISTS user_metrics (
                    user_id TEXT PRIMARY KEY,
                    total_orders INT,
                    total_spent DECIMAL,
                    avg_order_value DECIMAL,
                    last_order_date TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """)
            
            logger.info("Cassandra tables created successfully")
            
        except Exception as e:
            logger.error(f"Error creating Cassandra tables: {e}")
            
    def close_connections(self):
        """Close all database connections"""
        if self.redis_client:
            self.redis_client.close()
            logger.info("Redis connection closed")
            
        if self.cassandra_cluster:
            self.cassandra_cluster.shutdown()
            logger.info("Cassandra connection closed")
