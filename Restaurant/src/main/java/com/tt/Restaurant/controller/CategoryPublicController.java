package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.Category;
import com.tt.Restaurant.repository.CategoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryPublicController {

    private final CategoryRepository categoryRepository;

    public CategoryPublicController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Category> listActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByIdAsc();
    }
}
