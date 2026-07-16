package com.orderapp.event;

import com.orderapp.entity.Order;
import com.orderapp.entity.OrderItem;
import com.orderapp.entity.Product;
import com.orderapp.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCreatedEventTest {

    @Test
    void includesStableIdentifiersAndAnalyticsFields() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Wireless Bluetooth Headphones");

        Order order = new Order(user, "ORD-TEST-1");
        order.setId(UUID.randomUUID());
        order.setTaxAmount(new BigDecimal("8.00"));
        order.setShippingAmount(new BigDecimal("9.99"));
        order.addOrderItem(new OrderItem(product, 1, new BigDecimal("99.99")));

        OrderCreatedEvent event = new OrderCreatedEvent(order);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getOrderId()).isEqualTo(order.getId());
        assertThat(event.getTotalAmount()).isEqualByComparingTo("117.98");
        assertThat(event.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(product.getId().toString());
            assertThat(item.getProductName()).isEqualTo(product.getName());
            assertThat(item.getTotalPrice()).isEqualByComparingTo("99.99");
        });
    }
}
