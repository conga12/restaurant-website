package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.ReservationMapper;
import com.tt.Restaurant.dto.ReservationRequestDTO;
import com.tt.Restaurant.dto.ReservationResponseDTO;
import com.tt.Restaurant.dto.TableDTO;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.service.EmailService;
import com.tt.Restaurant.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tt.Restaurant.service.VNPayService;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/admin/api/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final EmailService emailService;
    private final VNPayService vnPayService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public AdminReservationController(
            ReservationService reservationService,
            EmailService emailService,
            VNPayService vnPayService
    ) {
        this.reservationService = reservationService;
        this.emailService = emailService;
        this.vnPayService = vnPayService;
    }

    @GetMapping
    public Page<ReservationResponseDTO> getPagedReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String date,           // yyyy-MM-dd
            @RequestParam(required = false) String status,         // PENDING, CANCELLED...
            @RequestParam(required = false) String keyword
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")); // mới nhất lên đầu!
        Page<Reservation> resvPage = reservationService.searchReservations(date, status, keyword, pageable);
        return resvPage.map(ReservationMapper::toResponseDTO);
    }

    @GetMapping("/unread-count")
    public long getUnreadReservationCount() {
        return reservationService.countUnread(); // countBySeenFalse()
    }
    @PostMapping("/mark-all-seen")
    public ResponseEntity<Void> markAllSeen() {
        reservationService.markAllSeen();
        long unread = reservationService.countUnread(); // sẽ = 0
        messagingTemplate.convertAndSend("/topic/reservation-unread", unread);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ReservationResponseDTO getReservationById(@PathVariable Long id) {
        return ReservationMapper.toResponseDTO(reservationService.getReservationById(id));
    }

    @PostMapping
    public ReservationResponseDTO createReservation(@RequestBody ReservationRequestDTO requestDTO) {
        try {
            Reservation reservation = ReservationMapper.toEntity(requestDTO);
            Reservation saved = reservationService.createReservation(reservation);

            try {
                if (saved.getDepositRequired() != null && saved.getDepositRequired()
                        && saved.getCustomerEmail() != null && !saved.getCustomerEmail().isBlank()
                        && saved.getDepositAmount() != null && saved.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {

                    String paymentUrl = vnPayService.createPaymentUrl(
                            "RES_" + saved.getId(),
                            saved.getDepositAmount().longValue(),
                            null
                    );

                    emailService.sendDepositRequestEmail(
                            saved.getCustomerEmail(),
                            saved.getCustomerName(),
                            paymentUrl,
                            saved.getReservationDate() != null ? saved.getReservationDate().toString() : "",
                            saved.getReservationTime() != null ? saved.getReservationTime().toString() : ""
                    );
                }
            } catch (Exception e) {
                System.out.println("Lỗi gửi mail: " + e.getMessage());
                e.printStackTrace();
            }
            // PUSH WEBSOCKET Ở ĐÂY!
            //log.info("THONG BAO PUSH BADGE!!!");
            messagingTemplate.convertAndSend("/topic/reservation-unread", "updated");
            return ReservationMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi tạo đặt bàn";

            if (msg.contains("Bàn đã được đặt")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
            }
            if (msg.contains("không tìm thấy")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
            }
            if (msg.contains("không hợp lệ") || msg.contains("không được để trống") || msg.contains("không thể")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
    }

    @PutMapping("/{id}")
    public ReservationResponseDTO updateReservation(@PathVariable Long id,
                                                    @RequestBody ReservationRequestDTO requestDTO) {
        try {
            Reservation reservation = ReservationMapper.toEntity(requestDTO);
            Reservation updated = reservationService.updateReservation(id, reservation);
            return ReservationMapper.toResponseDTO(updated);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi cập nhật đặt bàn";

            if (msg.contains("Bàn đã được đặt")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
            }
            if (msg.contains("không tìm thấy")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
    }

    @PutMapping("/{id}/confirm")
    public ReservationResponseDTO confirmReservation(@PathVariable Long id) {
        return ReservationMapper.toResponseDTO(reservationService.confirmReservation(id));
    }

    @PutMapping("/{id}/complete")
    public ReservationResponseDTO completeReservation(@PathVariable Long id) {
        return ReservationMapper.toResponseDTO(reservationService.completeReservation(id));
    }

    @PutMapping("/{id}/cancel")
    public ReservationResponseDTO cancelReservation(@PathVariable Long id) {
        return ReservationMapper.toResponseDTO(reservationService.cancelReservation(id));
    }

    @GetMapping("/available-tables")
    public List<TableDTO> getAvailableTables(@RequestParam LocalDate date,
                                             @RequestParam LocalTime time,
                                             @RequestParam Integer guests,
                                             @RequestParam(required = false) Long excludeReservationId) {
        return reservationService.getAvailableTables(date, time, guests, excludeReservationId)
                .stream()
                .map(ReservationMapper::toTableDTO)
                .toList();
    }
}