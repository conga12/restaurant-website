package com.tt.Restaurant.service;

import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.RestaurantTable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationService {
    List<Reservation> getAllReservations();

    Reservation getReservationById(Long id);

    Reservation createReservation(Reservation reservation);

    Reservation updateReservation(Long id, Reservation reservation);

    Reservation confirmReservation(Long id);

    Reservation completeReservation(Long id);

    Reservation cancelReservation(Long id);

    List<RestaurantTable> getAvailableTables(LocalDate date, LocalTime time, Integer guests);
    List<RestaurantTable> getAvailableTables(LocalDate date, LocalTime time, Integer guests, Long excludeReservationId);

    List<Reservation> getReservationsOfCurrentUser(String email);

    Reservation getReservationByIdForCurrentUser(Long id, String email);

    Reservation createReservationForCurrentUser(Reservation reservation, String email);

    Reservation cancelReservationForCurrentUser(Long id, String email);

    Page<Reservation> getAllReservations(Pageable pageable);

    Page<Reservation> searchReservations(String date, String status, String keyword, Pageable pageable);

    long countUnread();

    int markAllSeen();

}