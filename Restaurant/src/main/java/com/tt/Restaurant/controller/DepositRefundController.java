package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.service.EmailService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/deposits")
public class DepositRefundController {

    private final ReservationRepository reservationRepository;
    private final EmailService emailService;

    public DepositRefundController(ReservationRepository reservationRepository, EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
    }

    @PostMapping("/{reservationId}/refund")
    public Map<String, String> refundDeposit(@PathVariable Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId.longValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy reservation"));

        if (reservation.getDepositRequired() == null || !reservation.getDepositRequired()) {
            throw new RuntimeException("Đặt bàn này không yêu cầu cọc");
        }

        if (reservation.getDepositStatus() != Reservation.DepositStatus.PAID) {
            throw new RuntimeException("Chỉ hoàn cọc cho reservation đã cọc");
        }

        reservation.setDepositStatus(Reservation.DepositStatus.REFUNDED);
        reservationRepository.save(reservation);

        if (reservation.getCustomerEmail() != null && !reservation.getCustomerEmail().isBlank()) {
            emailService.sendDepositRefundedEmail(
                    reservation.getCustomerEmail(),
                    reservation.getCustomerName(),
                    Math.toIntExact(reservation.getId()),
                    reservation.getDepositAmount()
            );
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Hoàn cọc thành công");
        return response;
    }
}