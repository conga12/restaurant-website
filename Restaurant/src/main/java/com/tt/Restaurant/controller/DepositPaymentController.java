package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/deposit")
public class DepositPaymentController {

    private final ReservationRepository reservationRepository;
    private final VNPayService vnPayService;

    public DepositPaymentController(ReservationRepository reservationRepository, VNPayService vnPayService) {
        this.reservationRepository = reservationRepository;
        this.vnPayService = vnPayService;
    }

    @PostMapping("/create")
    public Map<String, String> createDepositPayment(@RequestParam Long reservationId,
                                                    HttpServletRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy reservation"));

        if (reservation.getDepositRequired() == null || !reservation.getDepositRequired()) {
            throw new RuntimeException("Reservation này không yêu cầu cọc");
        }

        if (reservation.getDepositAmount() == null || reservation.getDepositAmount().doubleValue() <= 0) {
            throw new RuntimeException("Số tiền cọc không hợp lệ");
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                "RES_" + reservation.getId(),
                reservation.getDepositAmount().longValue(),
                request
        );

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        return response;
    }
}