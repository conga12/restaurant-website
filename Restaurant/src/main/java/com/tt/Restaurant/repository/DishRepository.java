package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByAvailableTrue();
    List<Dish> findByAvailableTrueOrderByIdAsc();
    List<Dish> findByCategory_IdAndAvailableTrueOrderByIdAsc(Long categoryId);
}