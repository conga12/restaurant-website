package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.Promotion;
import com.tt.Restaurant.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin
public class PromotionController {
    @Autowired
    private PromotionRepository promotionRepo;

    // Get all active promotions (cho khách)
    @GetMapping
    public List<Promotion> getAllActive() {
        return promotionRepo.findByIsActiveTrueOrderByStartDateDesc();
    }

    // Add new promotion (admin)
    @PostMapping
    public Promotion add(@RequestBody Promotion p) {
        return promotionRepo.save(p);
    }
    // Sửa promotion (ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Promotion edit(@PathVariable Long id, @RequestBody Promotion p) {
        p.setId(id);
        return promotionRepo.save(p);
    }

    // Xóa promotion (ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        promotionRepo.deleteById(id);
    }
    // Xem chi tiết
    @GetMapping("/{id}")
    public Promotion get(@PathVariable Long id) {
        return promotionRepo.findById(id).orElse(null);
    }
}