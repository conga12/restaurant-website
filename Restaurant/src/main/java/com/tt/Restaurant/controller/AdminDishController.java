package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.DishDTO;
import com.tt.Restaurant.model.Category;
import com.tt.Restaurant.model.Dish;
import com.tt.Restaurant.repository.CategoryRepository;
import com.tt.Restaurant.repository.DishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/api/dishes")
public class AdminDishController {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;

    public AdminDishController(DishRepository dishRepository, CategoryRepository categoryRepository) {
        this.dishRepository = dishRepository;
        this.categoryRepository = categoryRepository;
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
    public List<DishDTO> listAll() {
        return dishRepository.findAll().stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DishDTO create(@RequestBody DishDTO req) {
        if (req.getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId is required");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (req.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price is required");
        }

        Category cat = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));

        Dish d = new Dish();
        d.setCategory(cat);
        d.setName(req.getName().trim());
        d.setDescription(req.getDescription());
        d.setPrice(req.getPrice());
        d.setImageUrl(req.getImageUrl());
        d.setAvailable(req.getAvailable() == null ? true : req.getAvailable());

        return toDto(dishRepository.save(d));
    }

    @PutMapping("/{id}")
    public DishDTO update(@PathVariable Long id, @RequestBody DishDTO req) {
        Dish d = dishRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));

        if (req.getCategoryId() != null) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
            d.setCategory(cat);
        }
        if (req.getName() != null && !req.getName().isBlank()) d.setName(req.getName().trim());
        if (req.getDescription() != null) d.setDescription(req.getDescription());
        if (req.getPrice() != null) d.setPrice(req.getPrice());
        if (req.getImageUrl() != null) d.setImageUrl(req.getImageUrl());
        if (req.getAvailable() != null) d.setAvailable(req.getAvailable());

        return toDto(dishRepository.save(d));
    }

    // Soft delete -> available=false
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inactive(@PathVariable Long id) {
        Dish d = dishRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        d.setAvailable(false);
        dishRepository.save(d);
    }

    @PatchMapping("/{id}/activate")
    public DishDTO activate(@PathVariable Long id) {
        Dish d = dishRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
        d.setAvailable(true);
        return toDto(dishRepository.save(d));
    }
}