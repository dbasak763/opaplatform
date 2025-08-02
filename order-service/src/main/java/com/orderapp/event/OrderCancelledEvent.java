package com.orderapp.event;

import com.orderapp.entity.Order;

import java.math.BigDecimal;

/**
 * Event published when an order is cancelled
 */
public class OrderCancelledEvent extends OrderEvent {
    
    private String previousStatus;
    private BigDecimal refundAmount;
    private String cancellationReason;
    private boolean stockRestored;
    
    public OrderCancelledEvent() {
        super();
    }
    
    public OrderCancelledEvent(Order order, Order.OrderStatus previousStatus, String reason) {
        super(order, "ORDER_CANCELLED");
        this.previousStatus = previousStatus != null ? previousStatus.toString() : null;
        this.refundAmount = order.getTotalAmount();
        this.cancellationReason = reason;
        this.stockRestored = true; // Assuming stock is always restored on cancellation
    }
    
    public OrderCancelledEvent(Order order, Order.OrderStatus previousStatus) {
        this(order, previousStatus, "Order cancelled by user");
    }
    
    // Getters and Setters
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }
    
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public boolean isStockRestored() {
        return stockRestored;
    }
    
    public void setStockRestored(boolean stockRestored) {
        this.stockRestored = stockRestored;
    }
    
    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId=" + getOrderId() +
                ", orderNumber='" + getOrderNumber() + '\'' +
                ", previousStatus='" + previousStatus + '\'' +
                ", refundAmount=" + refundAmount +
                ", cancellationReason='" + cancellationReason + '\'' +
                ", stockRestored=" + stockRestored +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
