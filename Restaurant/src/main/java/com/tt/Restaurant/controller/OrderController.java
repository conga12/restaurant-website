package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.CreateQrOrderRequestDTO;
import com.tt.Restaurant.dto.OrderResponseDTO;
import com.tt.Restaurant.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // QR order
    @PostMapping("/from-qr")
    public OrderResponseDTO createQrOrder(@RequestBody CreateQrOrderRequestDTO request) {
        return orderService.createQrOrder(request);
    }

    // Admin xem order
    @GetMapping("/{id}")
    public OrderResponseDTO getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
    @GetMapping("/ping")
    public String ping() {
        return "QR order API OK";
    }
}