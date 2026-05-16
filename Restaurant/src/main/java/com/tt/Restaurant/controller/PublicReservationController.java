package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.ReservationMapper;
import com.tt.Restaurant.dto.ReservationResponseDTO;
import com.tt.Restaurant.dto.ReservationHistoryLookupRequest;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/public/reservations")
public class PublicReservationController {

    private final ReservationRepository reservationRepository;

    public PublicReservationController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @PostMapping("/history")
    public List<ReservationResponseDTO> history(@RequestBody ReservationHistoryLookupRequest req) {
        String email = req == null || req.getEmail() == null ? "" : req.getEmail().trim();
        String phone = req == null || req.getPhone() == null ? "" : req.getPhone().trim().replaceAll("\\s+", "");

        if (email.isBlank() && phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập email hoặc số điện thoại.");
        }

        List<Reservation> list = reservationRepository.findHistoryByEmailOrPhone(email, phone);

        return list.stream().map(ReservationMapper::toResponseDTO).toList();
    }
}