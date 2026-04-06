package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.TableDTO;
import com.tt.Restaurant.service.TableService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/tables")
public class CustomerTableController {

    private final TableService tableService;

    public CustomerTableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public List<TableDTO> getAllTables() {
        return tableService.getAllTables().stream()
                .map(table -> new TableDTO(
                        table.getId(),
                        table.getTableNumber(),
                        table.getCapacity(),
                        table.getTableType() != null ? table.getTableType().name() : "STANDARD",
                        table.getLocation(), // 👈 THÊM DÒNG NÀY
                        table.getStatus() != null ? table.getStatus().name() : "AVAILABLE"
                ))
                .toList();
    }
}