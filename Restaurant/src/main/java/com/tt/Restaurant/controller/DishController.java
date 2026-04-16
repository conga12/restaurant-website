package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.DishDTO;
import com.tt.Restaurant.model.Dish;
import com.tt.Restaurant.repository.DishRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    private DishDTO toDto(Dish d) {
        DishDTO dto = new DishDTO();
        dto.setId(d.getId());
        dto.setName(d.getName());
        dto.setDescription(d.getDescription());
        dto.setPrice(d.getPrice());
        dto.setImageUrl(d.getImageUrl());
        dto.setAvailable(d.getAvailable());

        if (d.getCategory() != null) {
            dto.setCategoryId(d.getCategory().getId());
            dto.setCategoryName(d.getCategory().getName());
        }
        return dto;
    }

    @GetMapping
    public List<DishDTO> getAllDishes(@RequestParam(required = false) Long categoryId) {
        List<Dish> list = (categoryId == null)
                ? dishRepository.findByAvailableTrueOrderByIdAsc()
                : dishRepository.findByCategory_IdAndAvailableTrueOrderByIdAsc(categoryId);

        return list.stream().map(this::toDto).toList();
    }
}