package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.*;
import com.tt.Restaurant.model.Orders;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.Review;
import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.*;
import com.tt.Restaurant.service.GeminiService;
import com.tt.Restaurant.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GeminiService geminiService;
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ReviewController(
            ReviewService reviewService,
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ReservationRepository reservationRepository,
            ReviewMediaRepository reviewMediaRepository,
            SimpMessagingTemplate messagingTemplate,
            GeminiService geminiService
    ) {
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.reviewMediaRepository = reviewMediaRepository;
        this.messagingTemplate = messagingTemplate;
        this.geminiService = geminiService;
    }


    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ReviewRequestDTO dto, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = null;
        Object principal = auth.getPrincipal();

        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String s) {
            email = s;
        }

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        Review saved = reviewService.createReviewForCurrentUser(dto, email);

        messagingTemplate.convertAndSend("/topic/reviews", Map.of(
                "type", "NEW_REVIEW",
                "reviewId", saved.getId()
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đánh giá thành công",
                "reviewId", saved.getId()
        ));
    }

    // 1) Recent reviews (public)
    @GetMapping("/recent")
    public ResponseEntity<List<ReviewPublicDTO>> recent(@RequestParam(defaultValue = "10") int limit) {
        int size = Math.max(1, Math.min(limit, 50));

        var page = reviewRepository.findAllByOrderByIdDesc(org.springframework.data.domain.PageRequest.of(0, size));
        var reviews = page.getContent();

        List<Long> ids = new java.util.ArrayList<>();
        for (var r : reviews) ids.add(r.getId());

        java.util.Map<Long, java.util.List<String>> mediaMap = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            var media = reviewMediaRepository.findByReview_IdIn(ids);
            for (var m : media) {
                Long reviewId = m.getReview().getId();
                mediaMap.computeIfAbsent(reviewId, k -> new java.util.ArrayList<>()).add(m.getUrl());
            }
        }

        List<ReviewPublicDTO> out = new java.util.ArrayList<>();
        for (var r : reviews) {
            // IMPORTANT: tránh EntityNotFound (user mồ côi)
            String authorName = "Khách";
            try {
                if (r.getUser() != null) {
                    if (r.getUser().getUsername() != null && !r.getUser().getUsername().isBlank()) {
                        authorName = r.getUser().getUsername();
                    } else if (r.getUser().getEmail() != null && !r.getUser().getEmail().isBlank()) {
                        authorName = r.getUser().getEmail();
                    }
                }
            } catch (Exception ignored) {
                authorName = "Khách";
            }

            ReviewPublicDTO dto = new ReviewPublicDTO(
                    r.getId(),
                    r.getRating(),
                    r.getComment(),
                    r.getOwnerReply(),
                    authorName,
                    r.getCreatedAt()
            );

            dto.setMediaUrls(mediaMap.getOrDefault(r.getId(), java.util.List.of()));
            out.add(dto);
        }

        return ResponseEntity.ok(out);
    }

    // 2) Stats (public)
    @GetMapping("/stats")
    public ResponseEntity<ReviewStatsDTO> stats() {
        Double avg = reviewRepository.getAverageRating();
        Long total = reviewRepository.getTotalReviews();
        Long rec = reviewRepository.getRecommendedCount();

        double avgVal = avg == null ? 0.0 : avg;
        long totalVal = total == null ? 0 : total;
        long recVal = rec == null ? 0 : rec;

        int percent = totalVal == 0 ? 0 : (int) Math.round((recVal * 100.0) / totalVal);

        return ResponseEntity.ok(new ReviewStatsDTO(avgVal, totalVal, percent));
    }

    // 3) Eligible targets (login required)
    @GetMapping("/eligible")
    public ResponseEntity<ReviewEligibleDTO> eligible(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String principalName = principal.getName();

        var optUser = (principalName.contains("@"))
                ? userRepository.findByEmail(principalName)
                : userRepository.findByUsername(principalName);

        if (optUser.isEmpty()) {
            // Không tìm thấy user tương ứng với principal -> trả 401 để FE hiểu là chưa login hợp lệ
            return ResponseEntity.status(401).build();
        }

        User user = optUser.get();

        List<ReviewEligibleDTO.EligibleItem> items = new ArrayList<>();

        List<Orders> servedOrders = orderRepository.findByUser_IdAndStatus(user.getId(), Orders.OrderStatus.SERVED);
        for (Orders o : servedOrders) {
            boolean reviewed = reviewRepository.existsByOrder_IdAndUser_Id(o.getId(), user.getId());
            if (reviewed) continue;

            BigDecimal totalAmount = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            String label = "Order #" + (o.getOrderCode() != null ? o.getOrderCode() : o.getId())
                    + " - " + totalAmount + "đ";

            items.add(new ReviewEligibleDTO.EligibleItem("order:" + o.getId(), label));
        }

        List<Reservation> completed = reservationRepository.findByUser_IdAndStatus(user.getId(), Reservation.ReservationStatus.COMPLETED);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Reservation r : completed) {
            boolean reviewed = reviewRepository.existsByReservation_IdAndUser_Id(r.getId(), user.getId());
            if (reviewed) continue;

            String label = "Reservation #" + r.getId()
                    + " - " + (r.getReservationDate() != null ? r.getReservationDate().format(df) : "")
                    + " " + (r.getReservationTime() != null ? r.getReservationTime().toString() : "");

            items.add(new ReviewEligibleDTO.EligibleItem("reservation:" + r.getId(), label));
        }

        return ResponseEntity.ok(new ReviewEligibleDTO(items));
    }
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> replyToReview(@PathVariable Long id, @RequestBody ReplyRequestDTO request) {
        try {
            // Tìm review theo ID
            Review review = reviewRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

            // Cập nhật trường ownerReply
            review.setOwnerReply(request.getOwnerReply());

            // Lưu lại vào DB
            reviewRepository.save(review);

            return ResponseEntity.ok(Collections.singletonMap("message", "Phản hồi thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // Chỉ hiển thị phần cần sửa/chen vào uploadMedia và constructor

    @PostMapping(value = "/media", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadMedia(
            @RequestParam("files") java.util.List<org.springframework.web.multipart.MultipartFile> files,
            Principal principal
    ) throws Exception {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("No files");
        }

        // validate size/type
        for (var f : files) {
            if (f.isEmpty()) return ResponseEntity.badRequest().body("Empty file");
            if (f.getSize() > 5 * 1024 * 1024) return ResponseEntity.badRequest().body("File too large (max 5MB)");
            String ct = f.getContentType();
            if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp"))) {
                return ResponseEntity.badRequest().body("Only jpg/png/webp allowed");
            }
        }

        // save files
        java.time.LocalDate now = java.time.LocalDate.now();
        String subDir = "reviews/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue());

        java.nio.file.Path root = java.nio.file.Path.of(uploadDir).toAbsolutePath().normalize();
        java.nio.file.Path dir = root.resolve(subDir);
        java.nio.file.Files.createDirectories(dir);

        java.util.List<String> urls = new java.util.ArrayList<>();
        for (var f : files) {
            String ct = f.getContentType();

            // ===== NEW: Gemini moderation image BEFORE saving =====
            byte[] bytes = f.getBytes();
            var mod = geminiService.moderateReviewImage(bytes, ct);
            if (mod != null && Boolean.TRUE.equals(mod.getShouldBlock())) {
                String reason = (mod.getBlockReason() == null || mod.getBlockReason().isBlank())
                        ? "Hình ảnh không phù hợp"
                        : mod.getBlockReason();
                return ResponseEntity.badRequest().body("Ảnh bị từ chối: " + reason);
            }

            String ext = switch (ct) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                default -> "jpg";
            };
            String filename = java.util.UUID.randomUUID() + "." + ext;

            java.nio.file.Path dest = dir.resolve(filename);
            f.transferTo(dest.toFile());

            // public URL
            urls.add("/uploads/" + subDir + "/" + filename);
        }

        return ResponseEntity.ok(new com.tt.Restaurant.dto.UploadReviewMediaResponse(urls));
    }
}
