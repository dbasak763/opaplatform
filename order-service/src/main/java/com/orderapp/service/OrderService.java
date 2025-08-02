package com.orderapp.service;

import com.orderapp.dto.CreateOrderRequest;
import com.orderapp.dto.OrderResponse;
import com.orderapp.entity.Order;
import com.orderapp.entity.OrderItem;
import com.orderapp.entity.Product;
import com.orderapp.entity.User;
import com.orderapp.exception.InsufficientStockException;
import com.orderapp.exception.ResourceNotFoundException;
import com.orderapp.repository.OrderRepository;
import com.orderapp.repository.ProductRepository;
import com.orderapp.repository.UserRepository;
import com.orderapp.service.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderEventPublisher eventPublisher;

    @Autowired
    public OrderService(OrderRepository orderRepository, 
                       UserRepository userRepository,
                       ProductRepository productRepository,
                       OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @CacheEvict(value = {"orders", "userOrders"}, allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request) {
        logger.info("Creating order for user: {}", request.getUserId());

        // Validate user exists
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // Create order
        Order order = new Order(user, generateOrderNumber());
        order.setTaxAmount(request.getTaxAmount());
        order.setShippingAmount(request.getShippingAmount());
        order.setShippingAddress(request.getShippingAddress());
        order.setNotes(request.getNotes());

        // Process order items
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            // Check stock availability
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                        product.getName(), product.getStockQuantity(), itemRequest.getQuantity()));
            }

            // Create order item
            OrderItem orderItem = new OrderItem(product, itemRequest.getQuantity(), product.getPrice());
            order.addOrderItem(orderItem);

            // Update product stock
            product.decreaseStock(itemRequest.getQuantity());
            productRepository.save(product);
        }

        // Calculate total amount
        order.calculateTotalAmount();

        // Save order
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with order number: {}", savedOrder.getOrderNumber());

        // Publish order created event
        eventPublisher.publishOrderCreated(savedOrder);

        return new OrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    // @Cacheable(value = "orders", key = "#orderId") // Temporarily disabled for testing
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return new OrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
        return new OrderResponse(order);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userOrders", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(OrderResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(OrderResponse::new);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
            .map(OrderResponse::new)
            .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"orders", "userOrders"}, allEntries = true)
    public OrderResponse updateOrderStatus(UUID orderId, Order.OrderStatus newStatus) {
        logger.info("Updating order {} status to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // Set timestamps based on status
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case PENDING:
                // No special handling for pending status
                break;
            case CONFIRMED:
                // No special handling for confirmed status
                break;
            case PROCESSING:
                // No special handling for processing status
                break;
            case SHIPPED:
                if (order.getShippedAt() == null) {
                    order.setShippedAt(now);
                }
                break;
            case DELIVERED:
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(now);
                }
                if (order.getShippedAt() == null) {
                    order.setShippedAt(now);
                }
                break;
            case CANCELLED:
                // Restore stock for cancelled orders
                if (oldStatus != Order.OrderStatus.CANCELLED) {
                    restoreStock(order);
                }
                break;
            case REFUNDED:
                // Restore stock for refunded orders
                if (oldStatus != Order.OrderStatus.REFUNDED && oldStatus != Order.OrderStatus.CANCELLED) {
                    restoreStock(order);
                }
                break;
        }

        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);

        // Publish order status changed event
        if (newStatus == Order.OrderStatus.CANCELLED) {
            eventPublisher.publishOrderCancelled(savedOrder, oldStatus, "Order cancelled by status update");
        } else {
            eventPublisher.publishOrderStatusChanged(savedOrder, oldStatus, null);
        }

        return new OrderResponse(savedOrder);
    }

    @Transactional
    @CacheEvict(value = {"orders", "userOrders"}, allEntries = true)
    public void cancelOrder(UUID orderId) {
        updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        List<Order.OrderStatus> completedStatuses = List.of(
            Order.OrderStatus.DELIVERED,
            Order.OrderStatus.SHIPPED,
            Order.OrderStatus.PROCESSING,
            Order.OrderStatus.CONFIRMED
        );
        BigDecimal revenue = orderRepository.getTotalRevenueByStatuses(completedStatuses);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order.OrderStatus> completedStatuses = List.of(
            Order.OrderStatus.DELIVERED,
            Order.OrderStatus.SHIPPED,
            Order.OrderStatus.PROCESSING,
            Order.OrderStatus.CONFIRMED
        );
        BigDecimal revenue = orderRepository.getTotalRevenueBetweenDatesAndStatuses(startDate, endDate, completedStatuses);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public long getOrderCountByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.valueOf((int) (Math.random() * 1000));
        return "ORD-" + timestamp + "-" + randomSuffix;
    }

    private void restoreStock(Order order) {
        logger.info("Restoring stock for cancelled order: {}", order.getOrderNumber());
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.increaseStock(item.getQuantity());
            productRepository.save(product);
        }
    }
}
