package com.tt.Restaurant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "category") // nên để lower-case cho MySQL/Linux
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Category() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setActive(Boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}