package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.AvailableTableDTO;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import com.tt.Restaurant.service.TableAvailabilityService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TableAvailabilityServiceImpl implements TableAvailabilityService {

    private final RestaurantTableRepository tableRepo;
    private final ReservationRepository reservationRepo;

    public TableAvailabilityServiceImpl(RestaurantTableRepository tableRepo, ReservationRepository reservationRepo) {
        this.tableRepo = tableRepo;
        this.reservationRepo = reservationRepo;
    }

    @Override
    public List<AvailableTableDTO> findAvailableTables(LocalDate date, LocalTime time, int guests) {
        List<RestaurantTable> candidates =
                tableRepo.findByCapacityGreaterThanEqualOrderByCapacityAscTableNumberAsc(guests);

        List<Reservation.ReservationStatus> active = List.of(
                Reservation.ReservationStatus.PENDING,
                Reservation.ReservationStatus.CONFIRMED
        );

        Set<Long> booked = new HashSet<>(reservationRepo.findBookedTableIds(date, time, active));

        return candidates.stream()
                .filter(t -> !booked.contains(t.getId()))
                .map(t -> new AvailableTableDTO(
                        t.getId(),
                        t.getTableNumber(),
                        t.getCapacity(),
                        t.getLocation(),
                        t.getTableType() == null ? "STANDARD" : t.getTableType().name()
                ))
                .toList();
    }

    @Override
    public Map<LocalTime, List<AvailableTableDTO>> findAvailableTablesByDay(LocalDate date, int guests, List<LocalTime> slots) {
        return slots.stream().collect(java.util.stream.Collectors.toMap(
                t -> t,
                t -> findAvailableTables(date, t, guests),
                (a, b) -> a,
                java.util.LinkedHashMap::new
        ));
    }
}