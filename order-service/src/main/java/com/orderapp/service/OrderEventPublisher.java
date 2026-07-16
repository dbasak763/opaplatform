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
import org.springframework.stereotype.Service;

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
     * Publish each event once. The analytics service consumes this stream.
     */
    private void publishEvent(OrderEvent event, String logMessage) {
        try {
            publishToTopic(orderEventsTopic, event, logMessage);
        } catch (Exception e) {
            logger.error("Failed to publish event: {}", event, e);
        }
    }

    /**
     * Publish event to specific topic
     */
    private void publishToTopic(String topic, OrderEvent event, String logMessage) {
        String key = event.getOrderId().toString();

        kafkaTemplate.send(topic, key, event).whenComplete((result, error) -> {
            if (error == null) {
                logger.info("{} - Topic: {}, Partition: {}, Offset: {}",
                        logMessage,
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to publish event to topic {}: {}", topic, event, error);
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
