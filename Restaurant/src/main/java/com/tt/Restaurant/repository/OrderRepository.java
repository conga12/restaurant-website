package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {

    // ===== PHẦN CŨ (GIỮ NGUYÊN) =====
    boolean existsByReservationId(Long reservationId);

    Optional<Orders> findByReservationId(Long reservationId);

    // ===== PHẦN MỚI (QR ORDER) =====
    List<Orders> findAllByOrderByCreatedAtDesc();

    // admin chưa đọc
    @Query("""
        SELECT COUNT(o)
        FROM Orders o
        WHERE o.adminSeen = false
          AND o.status IN ('PENDING', 'PREPARING')
    """)
    long countUnreadOrders();
}