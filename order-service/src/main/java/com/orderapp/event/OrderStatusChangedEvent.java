package com.orderapp.event;

import com.orderapp.entity.Order;

/**
 * Event published when an order status changes
 */
public class OrderStatusChangedEvent extends OrderEvent {
    
    private String previousStatus;
    private String newStatus;
    private String reason;
    
    public OrderStatusChangedEvent() {
        super();
    }
    
    public OrderStatusChangedEvent(Order order, Order.OrderStatus previousStatus, String reason) {
        super(order, "ORDER_STATUS_CHANGED");
        this.previousStatus = previousStatus != null ? previousStatus.toString() : null;
        this.newStatus = order.getStatus().toString();
        this.reason = reason;
    }
    
    public OrderStatusChangedEvent(Order order, Order.OrderStatus previousStatus) {
        this(order, previousStatus, null);
    }
    
    // Getters and Setters
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    @Override
    public String toString() {
        return "OrderStatusChangedEvent{" +
                "orderId=" + getOrderId() +
                ", orderNumber='" + getOrderNumber() + '\'' +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
