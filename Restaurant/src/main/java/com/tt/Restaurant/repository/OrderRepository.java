package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {

    // ===== PHẦN CŨ (SỬA CHO ĐÚNG MANYTOONE) =====
    boolean existsByReservation_Id(Long reservationId);

    Optional<Orders> findByReservation_Id(Long reservationId);

    // ===== PHẦN MỚI (QR ORDER) =====
    List<Orders> findAllByOrderByCreatedAtDesc();

    List<Orders> findByUser_IdAndStatus(Long userId, Orders.OrderStatus status);

    // admin chưa đọc
    @Query("""
        SELECT COUNT(o)
        FROM Orders o
        WHERE o.adminSeen = false
          AND o.status IN ('PENDING', 'PREPARING')
    """)
    long countUnreadOrders();
}