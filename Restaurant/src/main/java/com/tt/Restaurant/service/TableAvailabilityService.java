package com.tt.Restaurant.service;

import com.tt.Restaurant.dto.AvailableTableDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface TableAvailabilityService {
    List<AvailableTableDTO> findAvailableTables(LocalDate date, LocalTime time, int guests);
    Map<LocalTime, List<AvailableTableDTO>> findAvailableTablesByDay(LocalDate date, int guests, List<LocalTime> slots);
}