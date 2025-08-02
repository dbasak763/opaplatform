package com.orderapp.event;

import com.orderapp.entity.Order;
import com.orderapp.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Event published when a new order is created
 */
public class OrderCreatedEvent extends OrderEvent {
    
    private BigDecimal totalAmount;
    private String status;
    private List<OrderItemData> items;
    private String shippingAddress;
    private String notes;
    
    public OrderCreatedEvent() {
        super();
    }
    
    public OrderCreatedEvent(Order order) {
        super(order, "ORDER_CREATED");
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus().toString();
        this.items = order.getItems().stream()
                .map(OrderItemData::new)
                .collect(Collectors.toList());
        this.shippingAddress = formatAddress(order);
        this.notes = order.getNotes();
    }
    
    private String formatAddress(Order order) {
        if (order.getShippingAddress() == null) {
            return null;
        }
        return String.format("%s, %s, %s %s, %s",
                order.getShippingAddress().getStreetAddress(),
                order.getShippingAddress().getCity(),
                order.getShippingAddress().getState(),
                order.getShippingAddress().getPostalCode(),
                order.getShippingAddress().getCountry());
    }
    
    // Getters and Setters
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<OrderItemData> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemData> items) {
        this.items = items;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Simplified order item data for events
     */
    public static class OrderItemData {
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        
        public OrderItemData() {}
        
        public OrderItemData(OrderItem item) {
            this.productName = item.getProduct().getName();
            this.quantity = item.getQuantity();
            this.unitPrice = item.getUnitPrice();
            this.totalPrice = item.getTotalPrice();
        }
        
        // Getters and Setters
        public String getProductName() {
            return productName;
        }
        
        public void setProductName(String productName) {
            this.productName = productName;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
        
        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
        
        public BigDecimal getTotalPrice() {
            return totalPrice;
        }
        
        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }
    }
}
