from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import json
import asyncio
from datetime import datetime, timedelta
from typing import Dict, Any, List
import logging
from .models import OrderMetrics, ProductMetrics, UserMetrics, RealtimeStats
from .kafka_consumer import AnalyticsProcessor
from .database import DatabaseConnections

logger = logging.getLogger(__name__)

app = FastAPI(title="Order Analytics API", version="1.0.0")

# CORS middleware for frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:3001"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global instances
db_connections = DatabaseConnections()
analytics_processor = AnalyticsProcessor()
websocket_connections: List[WebSocket] = []

@app.on_event("startup")
async def startup_event():
    """Initialize database connections on startup"""
    db_connections.connect_redis()
    db_connections.connect_cassandra()
    analytics_processor.redis_client = db_connections.redis_client
    analytics_processor.cassandra_session = db_connections.cassandra_session

@app.on_event("shutdown")
async def shutdown_event():
    """Clean up connections on shutdown"""
    db_connections.close_connections()

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "services": {
            "redis": db_connections.redis_client is not None,
            "cassandra": db_connections.cassandra_session is not None
        }
    }

@app.get("/metrics/orders", response_model=OrderMetrics)
async def get_order_metrics():
    """Get overall order metrics"""
    try:
        metrics = analytics_processor.get_current_metrics()

        total_orders = metrics.get('total_orders', 0)
        total_revenue = metrics.get('total_revenue', 0.0)
        avg_order_value = metrics.get('avg_order_value', 0.0)
        if total_orders > 0 and avg_order_value == 0.0:
            avg_order_value = total_revenue / total_orders

        return OrderMetrics(
            total_orders=total_orders,
            total_revenue=total_revenue,
            avg_order_value=avg_order_value,
            cancelled_orders=metrics.get('cancelled_orders', 0),
            orders_by_status=metrics.get('orders_by_status', {}),
            orders_per_hour=metrics.get('orders_per_hour', {}),
            orders_per_minute=metrics.get('orders_per_minute', {}),
            revenue_per_hour=metrics.get('revenue_per_hour', {}),
            revenue_per_minute=metrics.get('revenue_per_minute', {})
        )
    except Exception as e:
        logger.error(f"Error getting order metrics: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve order metrics")

@app.get("/metrics/realtime", response_model=RealtimeStats)
async def get_realtime_stats():
    """Get real-time statistics"""
    try:
        metrics = analytics_processor.get_current_metrics()
        orders_per_minute_map = metrics.get('orders_per_minute', {})
        revenue_per_minute_map = metrics.get('revenue_per_minute', {})

        latest_minute_key = max(orders_per_minute_map.keys(), default=None)
        if latest_minute_key is None:
            latest_minute_key = max(revenue_per_minute_map.keys(), default=None)

        current_orders_per_minute = float(orders_per_minute_map.get(latest_minute_key, 0))
        revenue_per_minute = float(revenue_per_minute_map.get(latest_minute_key, 0.0))

        recent_orders: List[Dict[str, Any]] = []
        if db_connections.redis_client:
            recent_orders_data = db_connections.redis_client.lrange("recent_orders", 0, 99)
            for order in recent_orders_data[:10]:
                try:
                    recent_orders.append(json.loads(order))
                except json.JSONDecodeError:
                    continue

        # Mock top products (in real implementation, query from Cassandra)
        top_products = [
            ProductMetrics(
                product_id="1",
                product_name="Wireless Headphones",
                total_quantity_sold=150,
                total_revenue=14999.50,
                order_count=75
            ),
            ProductMetrics(
                product_id="2", 
                product_name="Phone Case",
                total_quantity_sold=200,
                total_revenue=3999.00,
                order_count=100
            )
        ]
        
        return RealtimeStats(
            current_orders_per_minute=current_orders_per_minute,
            revenue_per_minute=revenue_per_minute,
            active_users=25,  # Mock data
            top_products=top_products,
            recent_orders=recent_orders
        )
        
    except Exception as e:
        logger.error(f"Error getting realtime stats: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve realtime stats")

@app.get("/metrics/trends")
async def get_trends(interval: str = "hour", window: int = 24):
    """Get revenue and order trends for the selected interval"""
    try:
        metrics = analytics_processor.get_current_metrics()
        interval = interval.lower()
        if interval not in {"hour", "minute"}:
            raise HTTPException(status_code=400, detail="interval must be 'hour' or 'minute'")

        if interval == "minute":
            orders_map = metrics.get('orders_per_minute', {})
            revenue_map = metrics.get('revenue_per_minute', {})
            parse_format = '%Y-%m-%d-%H-%M'
            display_format = '%H:%M'
            default_window = 60
        else:
            orders_map = metrics.get('orders_per_hour', {})
            revenue_map = metrics.get('revenue_per_hour', {})
            parse_format = '%Y-%m-%d-%H'
            display_format = '%Y-%m-%d %H:00'
            default_window = 24

        window = max(1, window or default_window)

        bucket_keys = sorted(set(list(orders_map.keys()) + list(revenue_map.keys())))
        if not bucket_keys:
            return {"interval": interval, "data": []}

        bucket_keys = bucket_keys[-min(window, len(bucket_keys)) :]

        trend_data = []
        for key in bucket_keys:
            try:
                dt = datetime.strptime(key, parse_format)
                label = dt.strftime(display_format)
            except ValueError:
                label = key

            trend_data.append({
                "bucket": key,
                "label": label,
                "orders": float(orders_map.get(key, 0)),
                "revenue": float(revenue_map.get(key, 0.0))
            })

        return {
            "interval": interval,
            "data": trend_data
        }
        
    except Exception as e:
        logger.error(f"Error getting trends: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve trend data")

@app.get("/metrics/products/top")
async def get_top_products(limit: int = 10):
    """Get top performing products"""
    try:
        # In real implementation, query from Cassandra
        top_products = [
            {
                "product_id": "1",
                "product_name": "Wireless Headphones",
                "total_quantity_sold": 150,
                "total_revenue": 14999.50,
                "order_count": 75
            },
            {
                "product_id": "2",
                "product_name": "Phone Case", 
                "total_quantity_sold": 200,
                "total_revenue": 3999.00,
                "order_count": 100
            },
            {
                "product_id": "3",
                "product_name": "Laptop Stand",
                "total_quantity_sold": 80,
                "total_revenue": 7999.20,
                "order_count": 40
            }
        ]
        
        return {"products": top_products[:limit]}
        
    except Exception as e:
        logger.error(f"Error getting top products: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve top products")

@app.websocket("/ws/realtime")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket endpoint for real-time updates"""
    await websocket.accept()
    websocket_connections.append(websocket)
    
    try:
        while True:
            # Send real-time updates every 5 seconds
            await asyncio.sleep(5)
            
            # Get current metrics
            realtime_stats = await get_realtime_stats()
            
            # Send to client
            await websocket.send_text(realtime_stats.json())
            
    except WebSocketDisconnect:
        websocket_connections.remove(websocket)
        logger.info("WebSocket client disconnected")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        if websocket in websocket_connections:
            websocket_connections.remove(websocket)

async def broadcast_update(data: Dict[str, Any]):
    """Broadcast updates to all connected WebSocket clients"""
    if websocket_connections:
        message = json.dumps(data)
        for websocket in websocket_connections.copy():
            try:
                await websocket.send_text(message)
            except Exception as e:
                logger.error(f"Error sending WebSocket message: {e}")
                websocket_connections.remove(websocket)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8091)
