package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.ReservationMapper;
import com.tt.Restaurant.dto.ReservationRequestDTO;
import com.tt.Restaurant.dto.ReservationResponseDTO;
import com.tt.Restaurant.dto.TableDTO;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.service.ReservationService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/admin/api/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<ReservationResponseDTO> getAllReservations() {
        return reservationService.getAllReservations()
                .stream()
                .map(ReservationMapper::toResponseDTO)
                .toList();
    }

    @GetMapping("/unread-count")
    public long getUnreadReservationCount() {
        return reservationService.getAllReservations()
                .stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.PENDING)
                .count();
    }

    @GetMapping("/{id}")
    public ReservationResponseDTO getReservationById(@PathVariable Long id) {
        return ReservationMapper.toResponseDTO(reservationService.getReservationById(id));
    }

    @PostMapping
    public ReservationResponseDTO createReservation(@RequestBody ReservationRequestDTO requestDTO) {
        Reservation reservation = ReservationMapper.toEntity(requestDTO);
        Reservation saved = reservationService.createReservation(reservation);
        return ReservationMapper.toResponseDTO(saved);
    }

    @PutMapping("/{id}")
    public ReservationResponseDTO updateReservation(@PathVariable Long id,
                                                    @RequestBody ReservationRequestDTO requestDTO) {
        Reservation reservation = ReservationMapper.toEntity(requestDTO);
        Reservation updated = reservationService.updateReservation(id, reservation);
        return ReservationMapper.toResponseDTO(updated);
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
                                             @RequestParam Integer guests) {
        return reservationService.getAvailableTables(date, time, guests)
                .stream()
                .map(ReservationMapper::toTableDTO)
                .toList();
    }
}