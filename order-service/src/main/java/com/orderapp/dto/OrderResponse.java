package com.orderapp.dto;

import com.orderapp.entity.Address;
import com.orderapp.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderResponse {

    private UUID id;
    private String orderNumber;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private List<OrderItemResponse> items;
    private Order.OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal subtotal;
    private Address shippingAddress;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private int totalItems;

    // Constructors
    public OrderResponse() {}

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.userId = order.getUser().getId();
        this.userFullName = order.getUser().getFullName();
        this.userEmail = order.getUser().getEmail();
        this.items = order.getOrderItems().stream()
            .map(OrderItemResponse::new)
            .collect(Collectors.toList());
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.taxAmount = order.getTaxAmount();
        this.shippingAmount = order.getShippingAmount();
        this.subtotal = order.getSubtotal();
        this.shippingAddress = order.getShippingAddress();
        this.notes = order.getNotes();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.shippedAt = order.getShippedAt();
        this.deliveredAt = order.getDeliveredAt();
        this.totalItems = order.getTotalItems();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public Order.OrderStatus getStatus() { return status; }
    public void setStatus(Order.OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
}
