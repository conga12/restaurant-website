package com.tt.Restaurant.service;

import com.tt.Restaurant.dto.*;

public interface OrderService {
    VerifyReservationResponseDTO verifyReservation(VerifyReservationRequestDTO request);
    OrderResponseDTO createOrder(CreateOrderRequestDTO request);
    OrderResponseDTO createQrOrder(CreateQrOrderRequestDTO request);
    OrderResponseDTO getOrderByReservation(Integer reservationId);
    OrderResponseDTO getOrderById(Long orderId);

}