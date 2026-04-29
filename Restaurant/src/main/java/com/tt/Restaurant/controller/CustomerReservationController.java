package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.ReservationMapper;
import com.tt.Restaurant.dto.ReservationRequestDTO;
import com.tt.Restaurant.dto.ReservationResponseDTO;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/reservations")
public class CustomerReservationController {

    private final ReservationService reservationService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public CustomerReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ReservationResponseDTO createReservation(
            @RequestBody ReservationRequestDTO requestDTO,
            Authentication authentication // cho phép null
    ) {
        Reservation reservation = ReservationMapper.toEntity(requestDTO);
        Reservation saved;

        if (authentication != null) {
            String email = authentication.getName();
            saved = reservationService.createReservationForCurrentUser(reservation, email);
        } else {
            // Khách vãng lai, không có user gắn vào Reservation
            saved = reservationService.createReservation(reservation);
        }

        // Bắn websocket notify SAU khi lưu thành công
        messagingTemplate.convertAndSend("/topic/reservation-unread", "updated");

        // Trả về dữ liệu đúng
        return ReservationMapper.toResponseDTO(saved);
    }

    @GetMapping("/{id}")
    public ReservationResponseDTO getReservationById(@PathVariable Long id,
                                                     Authentication authentication) {
        return ReservationMapper.toResponseDTO(
                reservationService.getReservationByIdForCurrentUser(id, authentication.getName())
        );
    }

    @GetMapping
    public List<ReservationResponseDTO> getMyReservations(Authentication authentication) {
        return reservationService.getReservationsOfCurrentUser(authentication.getName())
                .stream()
                .map(ReservationMapper::toResponseDTO)
                .toList();
    }

    @PutMapping("/{id}/cancel")
    public ReservationResponseDTO cancelReservation(@PathVariable Long id,
                                                    Authentication authentication) {
        return ReservationMapper.toResponseDTO(
                reservationService.cancelReservationForCurrentUser(id, authentication.getName())
        );
    }
}