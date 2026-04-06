package com.tt.Restaurant.service;

import com.tt.Restaurant.model.RestaurantTable;

import java.util.List;

public interface TableService {
    List<RestaurantTable> getAllTables();
    RestaurantTable getTableById(Integer id);
    RestaurantTable createTable(RestaurantTable table);
    RestaurantTable updateTable(Integer id, RestaurantTable table);
    void deleteTable(Integer id);
}