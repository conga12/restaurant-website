package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.CategoryRequest;
import com.tt.Restaurant.model.Category;
import com.tt.Restaurant.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/api/categories")
public class CategoryAdminController {

    private final CategoryRepository categoryRepository;

    public CategoryAdminController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@RequestBody CategoryRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }

        Category c = new Category();
        c.setName(req.getName().trim());
        c.setDescription(req.getDescription());
        c.setImageUrl(req.getImageUrl());
        c.setActive(req.getActive() == null ? true : req.getActive());

        return categoryRepository.save(c);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @RequestBody CategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (req.getName() != null && !req.getName().isBlank()) c.setName(req.getName().trim());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getImageUrl() != null) c.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) c.setActive(req.getActive());

        return categoryRepository.save(c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inactive(@PathVariable Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        c.setActive(false);
        categoryRepository.save(c);
    }
    @PatchMapping("/{id}/activate")
    public Category activate(@PathVariable Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        c.setActive(true);
        return categoryRepository.save(c);
    }
}
