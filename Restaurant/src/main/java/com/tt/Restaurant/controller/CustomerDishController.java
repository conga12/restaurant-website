package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.DishDTO;
import com.tt.Restaurant.model.Dish;
import com.tt.Restaurant.repository.DishRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/dishes")
public class CustomerDishController {

    private final DishRepository dishRepository;

    public CustomerDishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @GetMapping
    public List<DishDTO> getAllDishes() {
        List<Dish> dishes = dishRepository.findAll();

        return dishes.stream().map(dish -> {
            DishDTO dto = new DishDTO();
            dto.setId(dish.getId());
            dto.setName(dish.getName());
            dto.setDescription(dish.getDescription());
            dto.setPrice(dish.getPrice());
            dto.setImageUrl(dish.getImageUrl());
            dto.setAvailable(dish.getAvailable());

            if (dish.getCategory() != null) {
                dto.setCategoryId(dish.getCategory().getId());
                dto.setCategoryName(dish.getCategory().getName());
            }

            return dto;
        }).toList();
    }
}