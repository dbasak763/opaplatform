-- Cassandra initialization script for analytics
-- This script will be executed when Cassandra starts

-- Create keyspace for analytics
CREATE KEYSPACE IF NOT EXISTS analytics
WITH REPLICATION = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
};

-- Use the analytics keyspace
USE analytics;

-- Create tables for analytics data
CREATE TABLE IF NOT EXISTS order_events (
    event_id UUID PRIMARY KEY,
    event_type TEXT,
    order_id TEXT,
    user_id TEXT,
    timestamp TIMESTAMP,
    data TEXT
);

CREATE TABLE IF NOT EXISTS order_metrics_hourly (
    date_hour TEXT PRIMARY KEY,
    order_count INT,
    total_revenue DECIMAL,
    avg_order_value DECIMAL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_metrics (
    product_id TEXT PRIMARY KEY,
    product_name TEXT,
    total_quantity_sold INT,
    total_revenue DECIMAL,
    order_count INT,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_metrics (
    user_id TEXT PRIMARY KEY,
    total_orders INT,
    total_spent DECIMAL,
    avg_order_value DECIMAL,
    last_order_date TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create additional tables for real-time analytics
CREATE TABLE IF NOT EXISTS daily_stats (
    date TEXT PRIMARY KEY,
    total_orders INT,
    total_revenue DECIMAL,
    unique_customers INT,
    avg_order_value DECIMAL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_sales_hourly (
    product_id TEXT,
    hour_bucket TEXT,
    quantity_sold INT,
    revenue DECIMAL,
    PRIMARY KEY (product_id, hour_bucket)
) WITH CLUSTERING ORDER BY (hour_bucket DESC);
