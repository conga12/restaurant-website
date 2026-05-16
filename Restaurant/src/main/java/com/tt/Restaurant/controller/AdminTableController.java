package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.TableDTO;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.service.TableService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Map;
@RestController
@RequestMapping("/admin/api/tables")
public class AdminTableController {

    private final TableService tableService;
    private final SimpMessagingTemplate messagingTemplate;

    public AdminTableController(TableService tableService, SimpMessagingTemplate messagingTemplate) {
        this.tableService = tableService;
        this.messagingTemplate = messagingTemplate;
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

    @GetMapping("/{id}")
    public TableDTO getTableById(@PathVariable Integer id) {
        RestaurantTable table = tableService.getTableById(id);

        return new TableDTO(
                table.getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getTableType() != null ? table.getTableType().name() : "STANDARD",
                table.getLocation(),
                table.getStatus() != null ? table.getStatus().name() : "AVAILABLE",
                table.getActive() != null ? table.getActive() : true
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantTable createTable(@RequestBody RestaurantTable table) {
        RestaurantTable saved = tableService.createTable(table);
        messagingTemplate.convertAndSend("/topic/tables", Map.of("type", "tables:created", "id", saved.getId()));
        return saved;
    }

    @PutMapping("/{id}")
    public RestaurantTable updateTable(@PathVariable Integer id, @RequestBody RestaurantTable table) {
        RestaurantTable updated = tableService.updateTable(id, table);
        messagingTemplate.convertAndSend("/topic/tables", Map.of("type", "tables:updated", "id", updated.getId()));
        return updated;
    }

    // Soft-delete (đánh dấu inactive)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inactiveTable(@PathVariable Integer id) {
        tableService.inactiveTable(id);
        messagingTemplate.convertAndSend("/topic/tables", Map.of("type", "tables:updated", "id", id));
    }

    // Kích hoạt lại
    @PatchMapping("/{id}/activate")
    public TableDTO activateTable(@PathVariable Integer id) {
        RestaurantTable t = tableService.activateTable(id);
        messagingTemplate.convertAndSend("/topic/tables", Map.of("type", "tables:updated", "id", t.getId()));
        return new TableDTO(
                t.getId(),
                t.getTableNumber(),
                t.getCapacity(),
                t.getTableType() != null ? t.getTableType().name() : "STANDARD",
                t.getLocation(),
                t.getStatus() != null ? t.getStatus().name() : "AVAILABLE",
                t.getActive() != null ? t.getActive() : true
        );
    }
}