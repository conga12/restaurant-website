package com.tt.Restaurant.service;

import com.tt.Restaurant.dto.ReviewRequestDTO;
import com.tt.Restaurant.model.Review;

public interface ReviewService {
    Review createReviewForCurrentUser(ReviewRequestDTO dto, String email);
    Review createReviewForGuestReservation(ReviewRequestDTO dto, String email, String phone);
}