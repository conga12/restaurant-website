package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByIsActiveTrueOrderByStartDateDesc();
    List<Promotion> findByIsActiveTrue();
}
