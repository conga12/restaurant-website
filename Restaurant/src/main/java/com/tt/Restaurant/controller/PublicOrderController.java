package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.CreateOrderByTokenRequestDTO;
import com.tt.Restaurant.dto.CreateOrderRequestDTO;
import com.tt.Restaurant.dto.OrderResponseDTO;
import com.tt.Restaurant.dto.VerifyTokenRequest;
import com.tt.Restaurant.model.OrderAccessToken;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.Reservation.ReservationStatus;
import com.tt.Restaurant.service.OrderMagicLinkService;
import com.tt.Restaurant.service.OrderService;
import com.tt.Restaurant.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/public/orders")
public class PublicOrderController {

    private final OrderMagicLinkService magicLinkService;
    private final ReservationService reservationService;
    private final OrderService orderService;

    public PublicOrderController(OrderMagicLinkService magicLinkService,
                                 ReservationService reservationService,
                                 OrderService orderService) {
        this.magicLinkService = magicLinkService;
        this.reservationService = reservationService;
        this.orderService = orderService;
    }

    @PostMapping("/verify-token")
    public Map<String, Object> verifyToken(@RequestBody VerifyTokenRequest req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu token");
        }

        OrderAccessToken t = magicLinkService.validateToken(req.getToken());
        Reservation r = reservationService.getReservationById(t.getReservationId());

        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đặt món khi đặt bàn đã được xác nhận");
        }

        // Trả đúng format cho user/assets/js/order.js
        return Map.of(
                "reservationId", r.getId(),
                "customerName", r.getCustomerName(),
                "customerPhone", r.getCustomerPhone(),
                "reservationDate", r.getReservationDate(),
                "reservationTime", r.getReservationTime(),
                "tableNumber", r.getTable() != null ? r.getTable().getTableNumber() : null
        );
    }

    @PostMapping
    public OrderResponseDTO createOrderByToken(@RequestBody CreateOrderByTokenRequestDTO req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu token");
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất 1 món");
        }

        OrderAccessToken t = magicLinkService.validateToken(req.getToken());
        Reservation r = reservationService.getReservationById(t.getReservationId());

        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đặt món khi đặt bàn đã được xác nhận");
        }

        // Map token request -> DTO mà OrderService hiện tại đang dùng
        CreateOrderRequestDTO mapped = new CreateOrderRequestDTO();
        mapped.setReservationId(Math.toIntExact(r.getId())); // r.getId() là Integer trong model Reservation của bạn
        mapped.setCustomerPhone(r.getCustomerPhone());
        mapped.setNote(req.getNote());
        mapped.setPaymentOption(req.getPaymentOption());
        mapped.setCouponCode(req.getCouponCode());
        mapped.setItems(req.getItems());

        return orderService.createOrder(mapped);
    }
}