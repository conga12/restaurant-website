package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.ReviewAIResult;
import com.tt.Restaurant.dto.ReviewRequestDTO;
import com.tt.Restaurant.model.*;
import com.tt.Restaurant.repository.OrderRepository;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.ReviewRepository;
import com.tt.Restaurant.repository.UserRepository;
import com.tt.Restaurant.service.EmailService;
import com.tt.Restaurant.service.GeminiService;
import com.tt.Restaurant.service.ReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Base64;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GeminiService geminiService;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ReservationRepository reservationRepository,
            EmailService emailService,
            SimpMessagingTemplate messagingTemplate,
            GeminiService geminiService
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
        this.geminiService = geminiService;
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
            throw new RuntimeException("Review phải gắn với Order hoặc Reservation (chỉ chọn 1)");
        }

        // ===== 1) MODERATION + ANALYSIS bằng Gemini (không phụ thuộc số sao) =====
        ReviewAIResult ai = geminiService.analyzeAndModerateReview(dto.getComment(), dto.getRating());

        if (ai != null && Boolean.TRUE.equals(ai.getShouldBlock())) {
            String reason = (ai.getBlockReason() == null || ai.getBlockReason().isBlank())
                    ? "Nội dung không phù hợp"
                    : ai.getBlockReason();
            throw new RuntimeException("Đánh giá bị từ chối: " + reason);
        }

        // ===== 2) Tạo review entity =====
        Review review = new Review();
        review.setUser(user);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        // set AI fields
        if (ai != null) {
            if (ai.getSentiment() != null) review.setAiSentiment(ai.getSentiment());
            if (ai.getSummary() != null) review.setAiSummary(ai.getSummary());
            if (ai.getOwnerReply() != null && !ai.getOwnerReply().isBlank()) {
                // ownerReply public (không nhắc coupon)
                review.setOwnerReply(ai.getOwnerReply());
            }
        }

        // ===== 3) Validate order/reservation + quyền =====
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

        // ===== 4) Save review trước để có reviewId =====
        Review saved = reviewRepository.save(review);

        // ===== 5) Save media URLs (ảnh đã upload) =====
        if (dto.getMediaUrls() != null) {
            for (String url : dto.getMediaUrls()) {
                if (url == null || url.isBlank()) continue;

                // NOTE: Chặn ảnh thô tục nên làm ở endpoint upload (trước khi trả url).
                // Ở đây chỉ lưu URL đã được upload/duyệt.
                saved.getMedia().add(new ReviewMedia(saved, url.trim()));
            }
            saved = reviewRepository.save(saved);
        }

        // ===== 6) Auto coupon theo (C): rating <=2 OR ai.negative==true =====
        boolean negativeByAi = ai != null && Boolean.TRUE.equals(ai.getNegative());
        boolean negative = (saved.getRating() != null && saved.getRating() <= 2) || negativeByAi;

        if (negative && !saved.isAutoReplySent()) {
            String couponCode = generateCouponCode();
            saved.setCouponCode(couponCode);
            saved.setCouponDiscountPercent(20);
            saved.setCouponExpiresAt(LocalDateTime.now().plusDays(7));
            saved.setCouponUsed(false);

            String expiresText = saved.getCouponExpiresAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // Gemini generate emailBody
            ReviewAIResult emailAi = geminiService.generateLowRatingReplyEmail(
                    user.getUsername(),
                    saved.getRating(),
                    saved.getComment(),
                    couponCode,
                    20,
                    expiresText
            );

            String emailBody = (emailAi != null && emailAi.getEmailBody() != null && !emailAi.getEmailBody().isBlank())
                    ? emailAi.getEmailBody()
                    : buildLowRatingEmailFallback(saved, couponCode, 20, expiresText);

            // ownerReply: ưu tiên cái đã set từ analyzeAndModerateReview; nếu rỗng thì fallback
            if (saved.getOwnerReply() == null || saved.getOwnerReply().isBlank()) {
                saved.setOwnerReply(buildOwnerReplyFallback(saved));
            }

            // gửi email
            try {
                String toEmail = user.getEmail();
                if (toEmail != null && !toEmail.isBlank()) {
                    emailService.sendLowRatingReviewAutoReply(toEmail, user.getUsername(), emailBody);
                }
            } catch (Exception e) {
                System.err.println("Send low rating email failed: " + e.getMessage());
            }

            // notify realtime cho admin
            messagingTemplate.convertAndSend(
                    "/topic/review-alert",
                    java.util.Map.of(
                            "type", "LOW_RATING_REVIEW",
                            "reviewId", saved.getId(),
                            "rating", saved.getRating(),
                            "negativeByAi", negativeByAi
                    )
            );

            // mark sent
            saved.setAutoReplySent(true);
            saved.setAutoReplySentAt(LocalDateTime.now());
            saved.setAutoReplyMessage(emailBody);

            saved = reviewRepository.save(saved);
        }

        return saved;
    }

    @Transactional
    public Review createReviewForGuestReservation(ReviewRequestDTO dto, String email, String phone) {

        String normEmail = email == null ? "" : email.trim();
        String normPhone = phone == null ? "" : phone.trim();

        if ((normEmail.isBlank()) && (normPhone.isBlank())) {
            throw new RuntimeException("Vui lòng nhập Email hoặc SĐT.");
        }

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("Rating không hợp lệ");
        }

        // Guest flow: CHỈ cho reservation
        boolean hasOrder = dto.getOrderId() != null;
        boolean hasReservation = dto.getReservationId() != null;

        if (hasOrder || !hasReservation) {
            throw new RuntimeException("Guest chỉ được đánh giá theo Reservation");
        }

        Reservation reservation = reservationRepository.findById(dto.getReservationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy reservation"));

        // chỉ review khi COMPLETED
        if (reservation.getStatus() != Reservation.ReservationStatus.COMPLETED) {
            throw new RuntimeException("Chỉ được đánh giá đặt bàn đã hoàn thành (COMPLETED)");
        }

        // verify giống booking: match email OR phone
        boolean matchEmail = !normEmail.isBlank()
                && reservation.getCustomerEmail() != null
                && reservation.getCustomerEmail().trim().equalsIgnoreCase(normEmail);

        boolean matchPhone = !normPhone.isBlank()
                && reservation.getCustomerPhone() != null
                && reservation.getCustomerPhone().trim().equals(normPhone);

        if (!matchEmail && !matchPhone) {
            throw new RuntimeException("Email/SĐT không khớp với đặt bàn này");
        }

        // ===== 1) MODERATION + ANALYSIS bằng Gemini (không phụ thuộc số sao) =====
        ReviewAIResult ai = geminiService.analyzeAndModerateReview(dto.getComment(), dto.getRating());

        if (ai != null && Boolean.TRUE.equals(ai.getShouldBlock())) {
            String reason = (ai.getBlockReason() == null || ai.getBlockReason().isBlank())
                    ? "Nội dung không phù hợp"
                    : ai.getBlockReason();
            throw new RuntimeException("Đánh giá bị từ chối: " + reason);
        }

        // ===== 2) Tạo review entity =====
        Review review = new Review();
        review.setUser(null); // guest
        review.setOrder(null);
        review.setReservation(reservation);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        // set AI fields
        if (ai != null) {
            if (ai.getSentiment() != null) review.setAiSentiment(ai.getSentiment());
            if (ai.getSummary() != null) review.setAiSummary(ai.getSummary());
            if (ai.getOwnerReply() != null && !ai.getOwnerReply().isBlank()) {
                review.setOwnerReply(ai.getOwnerReply());
            }
        }

        // ===== 3) Check đã review chưa? (guest) =====
        // Rule (2) của bạn: "từ giờ mỗi reservation chỉ review 1 lần" nên chặn ở đây.
        // Nếu bạn đang áp "cutoff" ở controller thì có thể bỏ dòng này, hoặc thay bằng check cutoff.
        LocalDateTime cutoff = guestCutoff();
        if (reviewRepository.existsByReservation_IdAndCreatedAtGreaterThanEqual(reservation.getId(), cutoff)) {
            throw new RuntimeException("Reservation này đã được đánh giá rồi");
        }
        // ===== 4) Save review trước để có reviewId =====
        Review saved = reviewRepository.save(review);

        // ===== 5) Save media URLs (ảnh đã upload) =====
        if (dto.getMediaUrls() != null) {
            for (String url : dto.getMediaUrls()) {
                if (url == null || url.isBlank()) continue;
                saved.getMedia().add(new ReviewMedia(saved, url.trim()));
            }
            saved = reviewRepository.save(saved);
        }

        // ===== 6) Auto coupon theo (C): rating <=2 OR ai.negative==true =====
        boolean negativeByAi = ai != null && Boolean.TRUE.equals(ai.getNegative());
        boolean negative = (saved.getRating() != null && saved.getRating() <= 2) || negativeByAi;

        if (negative && !saved.isAutoReplySent()) {

            String couponCode = generateCouponCode();
            saved.setCouponCode(couponCode);
            saved.setCouponDiscountPercent(20);
            saved.setCouponExpiresAt(LocalDateTime.now().plusDays(7));
            saved.setCouponUsed(false);

            String expiresText = saved.getCouponExpiresAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // Guest không có user -> dùng customerName/email/phone
            String guestName = (reservation.getCustomerName() != null && !reservation.getCustomerName().isBlank())
                    ? reservation.getCustomerName()
                    : "Khách";

            // email body: nếu có email thì generate+send
            ReviewAIResult emailAi = geminiService.generateLowRatingReplyEmail(
                    guestName,
                    saved.getRating(),
                    saved.getComment(),
                    couponCode,
                    20,
                    expiresText
            );

            String emailBody = (emailAi != null && emailAi.getEmailBody() != null && !emailAi.getEmailBody().isBlank())
                    ? emailAi.getEmailBody()
                    : buildLowRatingEmailFallback(saved, couponCode, 20, expiresText);

            // ownerReply: ưu tiên cái đã set từ analyzeAndModerateReview; nếu rỗng thì fallback
            if (saved.getOwnerReply() == null || saved.getOwnerReply().isBlank()) {
                saved.setOwnerReply(buildOwnerReplyFallback(saved));
            }

            // gửi email: chỉ khi reservation có customerEmail
            try {
                String toEmail = reservation.getCustomerEmail();
                if (toEmail != null && !toEmail.isBlank()) {
                    emailService.sendLowRatingReviewAutoReply(toEmail, guestName, emailBody);
                }
            } catch (Exception e) {
                System.err.println("Send low rating email failed: " + e.getMessage());
            }

            // notify realtime cho admin
            messagingTemplate.convertAndSend(
                    "/topic/review-alert",
                    java.util.Map.of(
                            "type", "LOW_RATING_REVIEW",
                            "reviewId", saved.getId(),
                            "rating", saved.getRating(),
                            "negativeByAi", negativeByAi
                    )
            );

            // mark sent
            saved.setAutoReplySent(true);
            saved.setAutoReplySentAt(LocalDateTime.now());
            saved.setAutoReplyMessage(emailBody);

            saved = reviewRepository.save(saved);
        }

        return saved;
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

    @Value("${app.review.guest-cutoff:2026-05-06T00:00:00}")
    private String guestCutoffRaw;

    private LocalDateTime guestCutoff() {
        return LocalDateTime.parse(guestCutoffRaw);
    }

}