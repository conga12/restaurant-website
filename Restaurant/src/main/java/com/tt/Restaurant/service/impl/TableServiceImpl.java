package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import com.tt.Restaurant.service.TableService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableServiceImpl implements TableService {

    private final RestaurantTableRepository tableRepository;

    public TableServiceImpl(RestaurantTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Override
    public List<RestaurantTable> getAllTables() {
        return tableRepository.findAll();
    }

    @Override
    public RestaurantTable getTableById(Integer id) {
        return tableRepository.findById(id.longValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));
    }

    @Override
    public RestaurantTable createTable(RestaurantTable table) {
        return tableRepository.save(table);
    }

    @Override
    public RestaurantTable updateTable(Integer id, RestaurantTable table) {
        RestaurantTable existing = tableRepository.findById(id.longValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));

        existing.setTableNumber(table.getTableNumber());
        existing.setCapacity(table.getCapacity());
        existing.setLocation(table.getLocation());
        existing.setStatus(table.getStatus());
        existing.setTableType(table.getTableType());

        return tableRepository.save(existing);
    }

    @Override
    public void deleteTable(Integer id) {
        RestaurantTable existing = tableRepository.findById(id.longValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));
        tableRepository.delete(existing);
    }
}