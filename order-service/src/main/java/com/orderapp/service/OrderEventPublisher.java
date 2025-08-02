package com.orderapp.service;

import com.orderapp.entity.Order;
import com.orderapp.event.OrderCancelledEvent;
import com.orderapp.event.OrderCreatedEvent;
import com.orderapp.event.OrderEvent;
import com.orderapp.event.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Service for publishing order events to Kafka
 */
@Service
public class OrderEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${app.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;
    
    @Value("${app.kafka.topics.order-analytics:order-analytics}")
    private String orderAnalyticsTopic;
    
    /**
     * Publish order created event
     */
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        publishEvent(event, "Order created event published for order: " + order.getOrderNumber());
    }
    
    /**
     * Publish order status changed event
     */
    public void publishOrderStatusChanged(Order order, Order.OrderStatus previousStatus, String reason) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(order, previousStatus, reason);
        publishEvent(event, "Order status changed event published for order: " + order.getOrderNumber() + 
                " from " + previousStatus + " to " + order.getStatus());
    }
    
    /**
     * Publish order cancelled event
     */
    public void publishOrderCancelled(Order order, Order.OrderStatus previousStatus, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(order, previousStatus, reason);
        publishEvent(event, "Order cancelled event published for order: " + order.getOrderNumber());
    }
    
    /**
     * Generic method to publish events to both topics
     */
    private void publishEvent(OrderEvent event, String logMessage) {
        try {
            // Publish to main order events topic
            publishToTopic(orderEventsTopic, event, logMessage);
            
            // Also publish to analytics topic for real-time processing
            publishToTopic(orderAnalyticsTopic, event, "Analytics event: " + logMessage);
            
        } catch (Exception e) {
            logger.error("Failed to publish event: {}", event, e);
        }
    }
    
    /**
     * Publish event to specific topic
     */
    private void publishToTopic(String topic, OrderEvent event, String logMessage) {
        String key = event.getOrderId().toString();
        
        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onSuccess(SendResult<String, Object> result) {
                logger.info("{} - Topic: {}, Partition: {}, Offset: {}", 
                        logMessage, 
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
            
            @Override
            public void onFailure(Throwable ex) {
                logger.error("Failed to publish event to topic {}: {}", topic, event, ex);
            }
        });
    }
    
    /**
     * Publish custom event with specific topic
     */
    public void publishCustomEvent(String topic, OrderEvent event) {
        try {
            publishToTopic(topic, event, "Custom event published");
        } catch (Exception e) {
            logger.error("Failed to publish custom event to topic {}: {}", topic, event, e);
        }
    }
}
