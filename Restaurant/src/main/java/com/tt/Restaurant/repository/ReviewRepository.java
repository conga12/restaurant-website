package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrder_IdAndUser_Id(Long orderId, Long userId);
    boolean existsByReservation_IdAndUser_Id(Long reservationId, Long userId);

    Optional<Review> findTopByOrder_IdAndUser_IdOrderByIdDesc(Long orderId, Long userId);
    Optional<Review> findTopByReservation_IdAndUser_IdOrderByIdDesc(Long reservationId, Long userId);
    Optional<Review> findByCouponCodeIgnoreCase(String couponCode);

    // recent reviews
    Page<Review> findAllByOrderByIdDesc(Pageable pageable);

    // stats
    @Query("select avg(r.rating) from Review r")
    Double getAverageRating();

    @Query("select count(r) from Review r")
    Long getTotalReviews();

    @Query("select count(r) from Review r where r.rating >= 4")
    Long getRecommendedCount();

    // trong ReviewRepository
    @Query("SELECT r.rating as rating, COUNT(r) as cnt FROM Review r GROUP BY r.rating")
    List<Object[]> findRatingDistribution();

    boolean existsByReservation_Id(Long reservationId);

    boolean existsByReservation_IdAndCreatedAtGreaterThanEqual(Long reservationId, LocalDateTime cutoff);
}