package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.CreateOrderRequestDTO;
import com.tt.Restaurant.dto.OrderResponseDTO;
import com.tt.Restaurant.dto.VerifyReservationRequestDTO;
import com.tt.Restaurant.dto.VerifyReservationResponseDTO;
import com.tt.Restaurant.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/verify-reservation")
    public VerifyReservationResponseDTO verifyReservation(@RequestBody VerifyReservationRequestDTO request) {
        return orderService.verifyReservation(request);
    }

    @PostMapping
    public OrderResponseDTO createOrder(@RequestBody CreateOrderRequestDTO request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/reservation/{reservationId}")
    public OrderResponseDTO getOrderByReservation(@PathVariable Integer reservationId) {
        return orderService.getOrderByReservation(reservationId);
    }
    @GetMapping("/{orderId}")
    public OrderResponseDTO getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }
}