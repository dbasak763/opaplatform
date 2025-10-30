from pydantic import BaseModel
from datetime import datetime
from typing import Optional, Dict, Any
from enum import Enum

class OrderStatus(str, Enum):
    PENDING = "PENDING"
    CONFIRMED = "CONFIRMED"
    PROCESSING = "PROCESSING"
    SHIPPED = "SHIPPED"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"

class OrderEvent(BaseModel):
    event_id: str
    event_type: str
    order_id: str
    user_id: str
    timestamp: datetime
    data: Dict[str, Any]

class OrderMetrics(BaseModel):
    total_orders: int
    total_revenue: float
    avg_order_value: float
    cancelled_orders: int
    orders_by_status: Dict[str, int]
    orders_per_hour: Dict[str, int]
    orders_per_minute: Dict[str, int]
    revenue_per_hour: Dict[str, float]
    revenue_per_minute: Dict[str, float]

class ProductMetrics(BaseModel):
    product_id: str
    product_name: str
    total_quantity_sold: int
    total_revenue: float
    order_count: int

class UserMetrics(BaseModel):
    user_id: str
    total_orders: int
    total_spent: float
    avg_order_value: float
    last_order_date: Optional[datetime]

class RealtimeStats(BaseModel):
    current_orders_per_minute: float
    revenue_per_minute: float
    active_users: int
    top_products: list[ProductMetrics]
    recent_orders: list[Dict[str, Any]]
