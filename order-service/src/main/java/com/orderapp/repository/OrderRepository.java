package com.orderapp.repository;

import com.orderapp.entity.Order;
import com.orderapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<Order> findByUser(User user);

    List<Order> findByUserId(UUID userId);

    List<Order> findByStatus(Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:userId IS NULL OR o.user.id = :userId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate) AND " +
           "(:minAmount IS NULL OR o.totalAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR o.totalAmount <= :maxAmount)")
    Page<Order> findOrdersWithFilters(@Param("userId") UUID userId,
                                     @Param("status") Order.OrderStatus status,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     @Param("minAmount") BigDecimal minAmount,
                                     @Param("maxAmount") BigDecimal maxAmount,
                                     Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Order.OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status IN :statuses")
    BigDecimal getTotalRevenueByStatuses(@Param("statuses") List<Order.OrderStatus> statuses);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status IN :statuses")
    BigDecimal getTotalRevenueBetweenDatesAndStatuses(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     @Param("statuses") List<Order.OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<Order> findOldestOrdersByStatus(@Param("status") Order.OrderStatus status, Pageable pageable);

    @Query("SELECT DISTINCT EXTRACT(YEAR FROM o.createdAt) FROM Order o ORDER BY EXTRACT(YEAR FROM o.createdAt) DESC")
    List<Integer> findDistinctOrderYears();

    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH oi.product WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);
}
