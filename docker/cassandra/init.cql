-- Create keyspace for analytics
CREATE KEYSPACE IF NOT EXISTS analytics
WITH REPLICATION = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
};

USE analytics;

-- Order events table for real-time processing
CREATE TABLE IF NOT EXISTS order_events (
    event_id UUID PRIMARY KEY,
    order_id UUID,
    event_type TEXT,
    event_data TEXT,
    timestamp TIMESTAMP,
    user_id UUID
);

-- Aggregated metrics table
CREATE TABLE IF NOT EXISTS order_metrics (
    metric_date DATE,
    metric_hour INT,
    metric_type TEXT,
    metric_value DOUBLE,
    PRIMARY KEY (metric_date, metric_hour, metric_type)
);

-- Product performance table
CREATE TABLE IF NOT EXISTS product_metrics (
    product_id UUID,
    metric_date DATE,
    total_orders COUNTER,
    total_revenue DOUBLE,
    PRIMARY KEY (product_id, metric_date)
);

-- User activity table
CREATE TABLE IF NOT EXISTS user_activity (
    user_id UUID,
    activity_date DATE,
    order_count COUNTER,
    total_spent DOUBLE,
    PRIMARY KEY (user_id, activity_date)
);
