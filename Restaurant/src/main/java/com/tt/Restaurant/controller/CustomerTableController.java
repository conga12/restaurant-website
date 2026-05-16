package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.AvailableTableDTO;
import com.tt.Restaurant.dto.TableDTO;
import com.tt.Restaurant.service.TableAvailabilityService;
import com.tt.Restaurant.service.TableService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/customer/tables")
public class CustomerTableController {

    private final TableService tableService;
    private final TableAvailabilityService tableAvailabilityService;

    public CustomerTableController(TableService tableService, TableAvailabilityService tableAvailabilityService) {
        this.tableService = tableService;
        this.tableAvailabilityService = tableAvailabilityService;
    }

    @GetMapping
    public List<TableDTO> getAllTables() {
        return tableService.getAllTables().stream()
                .map(table -> new TableDTO(
                        table.getId(),
                        table.getTableNumber(),
                        table.getCapacity(),
                        table.getTableType() != null ? table.getTableType().name() : "STANDARD",
                        table.getLocation(),
                        table.getStatus() != null ? table.getStatus().name() : "AVAILABLE",
                        table.getActive() != null ? table.getActive() : true
                ))
                .toList();
    }

    // NEW: bàn trống theo lịch đặt
    @GetMapping("/available")
    public List<AvailableTableDTO> available(
            @RequestParam String date,   // yyyy-MM-dd
            @RequestParam String time,   // HH:mm
            @RequestParam int guests
    ) {
        LocalDate d = LocalDate.parse(date);
        LocalTime t = LocalTime.parse(time);
        return tableAvailabilityService.findAvailableTables(d, t, guests);
    }
}