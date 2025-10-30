package com.orderapp.controller;

import com.orderapp.dto.CreateOrderRequest;
import com.orderapp.dto.OrderResponse;
import com.orderapp.entity.Order;
import com.orderapp.service.OrderService;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Creating new order for user: {}", request.getUserId());
        OrderResponse order = orderService.createOrder(request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        logger.info("Fetching order with id: {}", orderId);
        OrderResponse order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(@PathVariable String orderNumber) {
        logger.info("Fetching order with order number: {}", orderNumber);
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        logger.info("Fetching all orders - page: {}, size: {}", page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderResponse>> getOrdersByUserId(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("Fetching orders for user: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getOrdersByUserId(userId, pageable);
        
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        logger.info("Fetching orders with status: {}", status);
        List<OrderResponse> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam Order.OrderStatus status) {
        
        logger.info("Updating order {} status to {}", orderId, status);
        OrderResponse order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderId) {
        logger.info("Cancelling order: {}", orderId);
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<BigDecimal> getTotalRevenue() {
        logger.info("Fetching total revenue");
        BigDecimal revenue = orderService.getTotalRevenue();
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/analytics/revenue/period")
    public ResponseEntity<BigDecimal> getRevenueBetweenDates(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        logger.info("Fetching revenue between {} and {}", startDate, endDate);
        
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
        
        BigDecimal revenue = orderService.getTotalRevenueBetweenDates(start, end);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/analytics/count/{status}")
    public ResponseEntity<Long> getOrderCountByStatus(@PathVariable Order.OrderStatus status) {
        logger.info("Fetching order count for status: {}", status);
        long count = orderService.getOrderCountByStatus(status);
        return ResponseEntity.ok(count);
    }
}
