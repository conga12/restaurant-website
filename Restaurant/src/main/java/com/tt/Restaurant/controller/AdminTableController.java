package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.TableDTO;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.service.TableService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/tables")
public class AdminTableController {

    private final TableService tableService;

    public AdminTableController(TableService tableService) {
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
                        table.getLocation(),
                        table.getStatus() != null ? table.getStatus().name() : "AVAILABLE"
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public TableDTO getTableById(@PathVariable Integer id) {
        RestaurantTable table = tableService.getTableById(id);

        return new TableDTO(
                table.getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getTableType() != null ? table.getTableType().name() : "STANDARD",
                table.getLocation(),
                table.getStatus() != null ? table.getStatus().name() : "AVAILABLE"
        );
    }

    @PostMapping
    public RestaurantTable createTable(@RequestBody RestaurantTable table) {
        return tableService.createTable(table);
    }

    @PutMapping("/{id}")
    public RestaurantTable updateTable(@PathVariable Integer id, @RequestBody RestaurantTable table) {
        return tableService.updateTable(id, table);
    }

    @DeleteMapping("/{id}")
    public void deleteTable(@PathVariable Integer id) {
        tableService.deleteTable(id);
    }
}