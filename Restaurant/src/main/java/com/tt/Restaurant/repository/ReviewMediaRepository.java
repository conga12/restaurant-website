package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    List<ReviewMedia> findByReview_IdIn(List<Long> reviewIds);
}