package com.tt.Restaurant.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;              // Tên chương trình
    @Column(length = 2000)
    private String description;        // Mô tả chi tiết
    private String imageUrl;           // Link ảnh đại diện
    private LocalDate startDate;       // Ngày bắt đầu
    private LocalDate endDate;         // Ngày kết thúc
    private Integer discountPercent;   // Phần trăm giảm
    private String code;               // Mã ưu đãi (nếu có)
    private String place;              // Địa điểm
    private Boolean isActive = true;   // Bật/tắt chương trình
    private LocalDateTime createdAt = LocalDateTime.now();

    // ==== Getters & Setters ====
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}

    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}

    public String getImageUrl() {return imageUrl;}
    public void setImageUrl(String imageUrl) {this.imageUrl = imageUrl;}

    public LocalDate getStartDate() {return startDate;}
    public void setStartDate(LocalDate startDate) {this.startDate = startDate;}

    public LocalDate getEndDate() {return endDate;}
    public void setEndDate(LocalDate endDate) {this.endDate = endDate;}

    public Integer getDiscountPercent() {return discountPercent;}
    public void setDiscountPercent(Integer discountPercent) {this.discountPercent = discountPercent;}

    public String getCode() {return code;}
    public void setCode(String code) {this.code = code;}

    public String getPlace() {return place;}
    public void setPlace(String place) {this.place = place;}

    public Boolean getIsActive() {return isActive;}
    public void setIsActive(Boolean isActive) {this.isActive = isActive;}

    public LocalDateTime getCreatedAt() {return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}