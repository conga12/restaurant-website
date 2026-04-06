package com.tt.Restaurant.dto;

import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.RestaurantTable;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationMapper {

    public static ReservationResponseDTO toResponseDTO(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();

        dto.setId(reservation.getId());
        dto.setCustomerName(reservation.getCustomerName());
        dto.setCustomerPhone(reservation.getCustomerPhone());
        dto.setCustomerEmail(reservation.getCustomerEmail());
        dto.setReservationDate(reservation.getReservationDate() != null ? reservation.getReservationDate().toString() : null);
        dto.setReservationTime(reservation.getReservationTime() != null ? reservation.getReservationTime().toString() : null);
        dto.setNumberOfGuests(reservation.getNumberOfGuests());
        dto.setStatus(reservation.getStatus() != null ? reservation.getStatus().name() : null);
        dto.setSpecialRequest(reservation.getSpecialRequest());
        dto.setCreatedAt(reservation.getCreatedAt() != null ? reservation.getCreatedAt().toString() : null);

        dto.setDepositRequired(reservation.getDepositRequired());
        dto.setDepositAmount(reservation.getDepositAmount());
        dto.setDepositStatus(reservation.getDepositStatus() != null ? reservation.getDepositStatus().name() : null);
        dto.setExpireAt(
                reservation.getExpireAt() != null ? reservation.getExpireAt().toString() : null
        );

        if (reservation.getTable() != null) {
            dto.setTableId(reservation.getTable().getId());
            dto.setTableNumber(reservation.getTable().getTableNumber());
            dto.setTableType(
                    reservation.getTable().getTableType() != null
                            ? reservation.getTable().getTableType().name()
                            : "STANDARD"
            );
        }

        return dto;
    }

    public static Reservation toEntity(ReservationRequestDTO dto) {
        Reservation reservation = new Reservation();

        reservation.setCustomerName(dto.getCustomerName());
        reservation.setCustomerPhone(dto.getCustomerPhone());
        reservation.setCustomerEmail(dto.getCustomerEmail());

        if (dto.getReservationDate() != null && !dto.getReservationDate().isBlank()) {
            reservation.setReservationDate(LocalDate.parse(dto.getReservationDate()));
        }

        if (dto.getReservationTime() != null && !dto.getReservationTime().isBlank()) {
            reservation.setReservationTime(LocalTime.parse(dto.getReservationTime()));
        }

        reservation.setNumberOfGuests(dto.getNumberOfGuests());
        reservation.setSpecialRequest(dto.getSpecialRequest());

        if (dto.getTableId() != null) {
            RestaurantTable table = new RestaurantTable();
            table.setId(dto.getTableId());
            reservation.setTable(table);
        }

        return reservation;
    }

    public static TableDTO toTableDTO(RestaurantTable table) {
        return new TableDTO(
                table.getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getTableType() != null ? table.getTableType().name() : "STANDARD",
                table.getLocation(),
                table.getStatus() != null ? table.getStatus().name() : "AVAILABLE"
        );
    }
}