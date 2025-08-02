package com.orderapp.service;

import com.orderapp.event.OrderCancelledEvent;
import com.orderapp.event.OrderCreatedEvent;
import com.orderapp.event.OrderEvent;
import com.orderapp.event.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Service for consuming order events from Kafka for analytics processing
 */
@Service
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    /**
     * Consume order events for general processing
     */
    @KafkaListener(topics = "order-events", groupId = "order-service-group")
    public void consumeOrderEvent(@Payload OrderEvent event,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {
        try {
            logger.info("Received order event: {} from topic: {}, partition: {}, offset: {}",
                    event.getEventType(), topic, partition, offset);

            processOrderEvent(event);

            // Acknowledge the message
            acknowledgment.acknowledge();
            logger.debug("Successfully processed and acknowledged order event: {}", event.getEventId());

        } catch (Exception e) {
            logger.error("Error processing order event: {}", event.getEventId(), e);
            // In a production environment, you might want to send to a dead letter queue
            // For now, we'll acknowledge to prevent infinite retries
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consume order events specifically for analytics processing
     */
    @KafkaListener(topics = "order-analytics", groupId = "analytics-service-group")
    public void consumeOrderAnalyticsEvent(@Payload OrderEvent event,
                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                           @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                           @Header(KafkaHeaders.OFFSET) long offset,
                                           Acknowledgment acknowledgment) {
        try {
            logger.info("Received analytics event: {} from topic: {}, partition: {}, offset: {}",
                    event.getEventType(), topic, partition, offset);

            processAnalyticsEvent(event);

            // Acknowledge the message
            acknowledgment.acknowledge();
            logger.debug("Successfully processed and acknowledged analytics event: {}", event.getEventId());

        } catch (Exception e) {
            logger.error("Error processing analytics event: {}", event.getEventId(), e);
            // In a production environment, you might want to send to a dead letter queue
            acknowledgment.acknowledge();
        }
    }

    /**
     * Process general order events
     */
    private void processOrderEvent(OrderEvent event) {
        switch (event.getEventType()) {
            case "ORDER_CREATED":
                handleOrderCreated((OrderCreatedEvent) event);
                break;
            case "ORDER_STATUS_CHANGED":
                handleOrderStatusChanged((OrderStatusChangedEvent) event);
                break;
            case "ORDER_CANCELLED":
                handleOrderCancelled((OrderCancelledEvent) event);
                break;
            default:
                logger.warn("Unknown event type: {}", event.getEventType());
        }
    }

    /**
     * Process analytics-specific events
     */
    private void processAnalyticsEvent(OrderEvent event) {
        switch (event.getEventType()) {
            case "ORDER_CREATED":
                processOrderCreatedAnalytics((OrderCreatedEvent) event);
                break;
            case "ORDER_STATUS_CHANGED":
                processOrderStatusChangedAnalytics((OrderStatusChangedEvent) event);
                break;
            case "ORDER_CANCELLED":
                processOrderCancelledAnalytics((OrderCancelledEvent) event);
                break;
            default:
                logger.warn("Unknown analytics event type: {}", event.getEventType());
        }
    }

    /**
     * Handle order created events
     */
    private void handleOrderCreated(OrderCreatedEvent event) {
        logger.info("Processing order created event for order: {} with total: {}",
                event.getOrderNumber(), event.getTotalAmount());
        
        // TODO: Implement order created processing logic
        // Examples:
        // - Send welcome email to customer
        // - Update inventory reservations
        // - Trigger fulfillment process
        // - Update customer order history
    }

    /**
     * Handle order status changed events
     */
    private void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        logger.info("Processing order status changed event for order: {} from {} to {}",
                event.getOrderNumber(), event.getPreviousStatus(), event.getNewStatus());
        
        // TODO: Implement status change processing logic
        // Examples:
        // - Send status update notifications
        // - Update external systems
        // - Trigger shipping processes
        // - Update customer communications
    }

    /**
     * Handle order cancelled events
     */
    private void handleOrderCancelled(OrderCancelledEvent event) {
        logger.info("Processing order cancelled event for order: {} with reason: {}",
                event.getOrderNumber(), event.getCancellationReason());
        
        // TODO: Implement cancellation processing logic
        // Examples:
        // - Process refunds
        // - Update inventory
        // - Send cancellation notifications
        // - Update customer service systems
    }

    /**
     * Process order created analytics
     */
    private void processOrderCreatedAnalytics(OrderCreatedEvent event) {
        logger.info("Processing analytics for order created: {} with value: {}",
                event.getOrderNumber(), event.getTotalAmount());
        
        // TODO: Implement analytics processing
        // Examples:
        // - Update real-time revenue metrics
        // - Track customer acquisition
        // - Update product popularity metrics
        // - Calculate conversion rates
    }

    /**
     * Process order status changed analytics
     */
    private void processOrderStatusChangedAnalytics(OrderStatusChangedEvent event) {
        logger.info("Processing analytics for order status change: {} to {}",
                event.getOrderNumber(), event.getNewStatus());
        
        // TODO: Implement analytics processing
        // Examples:
        // - Update order fulfillment metrics
        // - Track processing times
        // - Calculate status transition rates
        // - Update operational dashboards
    }

    /**
     * Process order cancelled analytics
     */
    private void processOrderCancelledAnalytics(OrderCancelledEvent event) {
        logger.info("Processing analytics for order cancellation: {} with reason: {}",
                event.getOrderNumber(), event.getCancellationReason());
        
        // TODO: Implement analytics processing
        // Examples:
        // - Track cancellation rates
        // - Analyze cancellation reasons
        // - Update refund metrics
        // - Monitor customer satisfaction impact
    }
}
