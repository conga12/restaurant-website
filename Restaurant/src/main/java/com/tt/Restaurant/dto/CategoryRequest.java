package com.tt.Restaurant.dto;

public class CategoryRequest {
    private String name;
    private String description;
    private String imageUrl;
    private Boolean active;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Boolean getActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setActive(Boolean active) { this.active = active; }
}