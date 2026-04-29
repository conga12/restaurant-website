package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.ReviewRequestDTO;
import com.tt.Restaurant.model.*;
import com.tt.Restaurant.repository.OrderRepository;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.ReviewRepository;
import com.tt.Restaurant.repository.UserRepository;
import com.tt.Restaurant.service.EmailService;
import com.tt.Restaurant.service.ReviewService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tt.Restaurant.dto.AiApologyResponse;
import com.tt.Restaurant.service.AiService;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import java.time.LocalDateTime;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiService aiService;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ReservationRepository reservationRepository,
            EmailService emailService,
            SimpMessagingTemplate messagingTemplate,
            AiService aiService
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public Review createReviewForCurrentUser(ReviewRequestDTO dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("Rating không hợp lệ");
        }

        boolean hasOrder = dto.getOrderId() != null;
        boolean hasReservation = dto.getReservationId() != null;

        if (hasOrder == hasReservation) {
            // hoặc cả hai cùng true, hoặc cả hai cùng false
            throw new RuntimeException("Review phải gắn với Order hoặc Reservation (chỉ chọn 1)");
        }

        Review review = new Review();
        review.setUser(user);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        if (hasOrder) {
            Orders order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

// check đúng chủ order
            if (order.getUser() == null || order.getUser().getId() == null || !order.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Bạn không có quyền đánh giá đơn hàng này");
            }

// check đã hoàn thành
            if (order.getStatus() != Orders.OrderStatus.SERVED) {
                throw new RuntimeException("Chỉ có thể đánh giá khi đơn hàng đã hoàn thành (SERVED)");
            }

            if (reviewRepository.existsByOrder_IdAndUser_Id(order.getId(), user.getId())) {
                throw new RuntimeException("Bạn đã review cho order này rồi");
            }

            review.setOrder(order);
        } else {
            Reservation reservation = reservationRepository.findById(dto.getReservationId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy reservation"));

            if (reviewRepository.existsByReservation_IdAndUser_Id(reservation.getId(), user.getId())) {
                throw new RuntimeException("Bạn đã review cho reservation này rồi");
            }

            review.setReservation(reservation);
        }

        Review saved = reviewRepository.save(review);
        if (dto.getMediaUrls() != null) {
            for (String url : dto.getMediaUrls()) {
                if (url == null || url.isBlank()) continue;
                saved.getMedia().add(new ReviewMedia(saved, url.trim()));
            }
            saved = reviewRepository.save(saved);
        }

        // Nếu rating thấp (<=2) => auto-message
        // Nếu rating thấp (<=2) => auto-message + coupon -20% + ownerReply
        if (saved.getRating() != null && saved.getRating() <= 2 && !saved.isAutoReplySent()) {

            // 1) coupon -20%
            String couponCode = generateCouponCode();
            saved.setCouponCode(couponCode);
            saved.setCouponDiscountPercent(20);
            saved.setCouponExpiresAt(LocalDateTime.now().plusDays(7));
            saved.setCouponUsed(false);

            String expiresText = saved.getCouponExpiresAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // 2) AI generate (fallback nếu lỗi)
            AiApologyResponse ai = null;
            try {
                ai = aiService.generateLowRatingReply(
                        user.getUsername(),
                        saved.getRating(),
                        saved.getComment(),
                        couponCode,
                        20,
                        expiresText
                );
            } catch (Exception ignored) {}

            String ownerReply = (ai != null && ai.getOwnerReply() != null && !ai.getOwnerReply().isBlank())
                    ? ai.getOwnerReply()
                    : buildOwnerReplyFallback(saved);

            String emailBody = (ai != null && ai.getEmailBody() != null && !ai.getEmailBody().isBlank())
                    ? ai.getEmailBody()
                    : buildLowRatingEmailFallback(saved, couponCode, 20, expiresText);

            saved.setOwnerReply(ownerReply);

            // 3) Email cho khách
            try {
                String toEmail = user.getEmail();
                if (toEmail != null && !toEmail.isBlank()) {
                    emailService.sendLowRatingReviewAutoReply(toEmail, user.getUsername(), emailBody);
                }
            } catch (Exception e) {
                System.err.println("Send low rating email failed: " + e.getMessage());
            }

            // 4) Notification realtime cho admin (JSON)
            messagingTemplate.convertAndSend(
                    "/topic/review-alert",
                    java.util.Map.of(
                            "type", "LOW_RATING_REVIEW",
                            "reviewId", saved.getId(),
                            "rating", saved.getRating()
                    )
            );

            // 5) Mark sent
            saved.setAutoReplySent(true);
            saved.setAutoReplySentAt(LocalDateTime.now());
            saved.setAutoReplyMessage(emailBody);

            saved = reviewRepository.save(saved);
        }

        return saved;
    }

    private String buildLowRatingAutoReply(Review review) {
        String name = review.getUser() != null ? review.getUser().getUsername() : "bạn";
        int rating = review.getRating() != null ? review.getRating() : 0;

        return "Chào " + (name == null ? "bạn" : name) + ",\n\n"
                + "Cảm ơn bạn đã để lại đánh giá. Rất tiếc vì trải nghiệm của bạn chưa tốt ("
                + rating + "/5).\n"
                + "Bạn có thể chia sẻ thêm chi tiết (món ăn/phục vụ/thời gian chờ/không gian) để bên mình kiểm tra và cải thiện không?\n\n"
                + "Trân trọng,\nRestaurantOS";
    }

    private String generateCouponCode() {
        return "SORRY-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String buildOwnerReplyFallback(Review review) {
        return "Nhà hàng rất tiếc vì trải nghiệm của bạn chưa tốt. Chúng tôi đã ghi nhận góp ý và sẽ cải thiện trong thời gian sớm nhất.";
    }

    private String buildLowRatingEmailFallback(Review review, String couponCode, int discountPercent, String expiresText) {
        String name = (review.getUser() != null && review.getUser().getUsername() != null)
                ? review.getUser().getUsername() : "bạn";
        int rating = review.getRating() != null ? review.getRating() : 0;

        return "Chào " + name + ",\n\n"
                + "Cảm ơn bạn đã để lại đánh giá. Rất tiếc vì trải nghiệm của bạn chưa tốt (" + rating + "/5).\n\n"
                + "Để xin lỗi, nhà hàng gửi bạn mã giảm giá " + discountPercent + "% cho order online:\n"
                + "- Mã: " + couponCode + "\n"
                + "- HSD: " + expiresText + "\n\n"
                + "Khi thanh toán, bạn nhập mã ở ô 'Mã giảm giá'.\n\n"
                + "Trân trọng,\nHƯƠNG VIỆT";
    }
}