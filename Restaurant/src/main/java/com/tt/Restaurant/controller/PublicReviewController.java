package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.PublicReviewLookupRequest;
import com.tt.Restaurant.dto.PublicReservationReviewCreateRequest;
import com.tt.Restaurant.dto.ReviewEligibleDTO;
import com.tt.Restaurant.dto.ReviewRequestDTO;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.Review;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.ReviewRepository;
import com.tt.Restaurant.service.ReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/public/reviews")
public class PublicReviewController {

    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.review.guest-cutoff:2026-05-06T00:00:00}")
    private String guestCutoffRaw;

    public PublicReviewController(
            ReservationRepository reservationRepository,
            ReviewRepository reviewRepository,
            ReviewService reviewService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.reservationRepository = reservationRepository;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.messagingTemplate = messagingTemplate;
    }

    private LocalDateTime guestCutoff() {
        return LocalDateTime.parse(guestCutoffRaw);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", "");
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @PostMapping("/eligible")
    public ResponseEntity<?> eligible(@RequestBody PublicReviewLookupRequest req) {
        String email = norm(req.getEmail());
        String phone = norm(req.getPhone());

        if (blank(email) && blank(phone)) {
            return ResponseEntity.badRequest().body("Vui lòng nhập Email hoặc SĐT.");
        }

        List<Reservation> completed = reservationRepository.findByEmailOrPhoneAndStatus(
                email, phone, Reservation.ReservationStatus.COMPLETED
        );

        LocalDateTime cutoff = guestCutoff();

        List<ReviewEligibleDTO.EligibleItem> items = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Reservation r : completed) {
            boolean reviewedAfterCutoff =
                    reviewRepository.existsByReservation_IdAndCreatedAtGreaterThanEqual(r.getId(), cutoff);

            if (reviewedAfterCutoff) continue;

            String label = "Reservation #" + r.getId()
                    + " - " + (r.getReservationDate() != null ? r.getReservationDate().format(df) : "")
                    + " " + (r.getReservationTime() != null ? r.getReservationTime().toString() : "");

            items.add(new ReviewEligibleDTO.EligibleItem("reservation:" + r.getId(), label));
        }

        if (items.isEmpty()) {
            return ResponseEntity.status(404).body("Không có đặt bàn hoàn thành để đánh giá.");
        }

        return ResponseEntity.ok(new ReviewEligibleDTO(items));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PublicReservationReviewCreateRequest req) {
        String email = norm(req.getEmail());
        String phone = norm(req.getPhone());

        if (blank(email) && blank(phone)) {
            return ResponseEntity.badRequest().body("Vui lòng nhập Email hoặc SĐT.");
        }

        if (req.getReservationId() == null) {
            return ResponseEntity.badRequest().body("Thiếu reservationId.");
        }

        Integer rating = req.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body("Rating không hợp lệ.");
        }

        if (blank(req.getComment())) {
            return ResponseEntity.badRequest().body("Vui lòng nhập nội dung đánh giá.");
        }

        Reservation r = reservationRepository.findById(req.getReservationId()).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();

        if (r.getStatus() != Reservation.ReservationStatus.COMPLETED) {
            return ResponseEntity.status(403).body("Chỉ được đánh giá đặt bàn đã hoàn thành.");
        }

        boolean matchEmail = !blank(email) && r.getCustomerEmail() != null
                && r.getCustomerEmail().trim().equalsIgnoreCase(email);
        boolean matchPhone = !blank(phone) && r.getCustomerPhone() != null
                && r.getCustomerPhone().trim().equals(phone);

        if (!matchEmail && !matchPhone) {
            return ResponseEntity.status(403).body("Email/SĐT không khớp với đặt bàn này.");
        }

        LocalDateTime cutoff = guestCutoff();
        if (reviewRepository.existsByReservation_IdAndCreatedAtGreaterThanEqual(r.getId(), cutoff)) {
            return ResponseEntity.badRequest().body("Đặt bàn này đã được đánh giá rồi.");
        }

        try {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setReservationId(r.getId());
            dto.setRating(rating);
            dto.setComment(req.getComment().trim());
            dto.setMediaUrls(req.getMediaUrls());

            // QUAN TRỌNG: gọi service để chạy AI/Gemini
            Review saved = reviewService.createReviewForGuestReservation(dto, email, phone);

            messagingTemplate.convertAndSend("/topic/reviews", Map.of(
                    "type", "NEW_REVIEW",
                    "reviewId", saved.getId()
            ));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đánh giá thành công",
                    "reviewId", saved.getId()
            ));
        } catch (RuntimeException ex) {
            // để FE hiện message rõ ràng
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}