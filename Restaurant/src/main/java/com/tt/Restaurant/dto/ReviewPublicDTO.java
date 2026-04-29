package com.tt.Restaurant.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewPublicDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private String ownerReply;
    private String authorName;
    private LocalDateTime createdAt;

    // NEW
    private List<String> mediaUrls = new ArrayList<>();

    public ReviewPublicDTO() {}

    public ReviewPublicDTO(Long id, Integer rating, String comment, String ownerReply, String authorName, LocalDateTime createdAt) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.ownerReply = ownerReply;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public String getOwnerReply() { return ownerReply; }
    public String getAuthorName() { return authorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<String> getMediaUrls() { return mediaUrls; }

    public void setId(Long id) { this.id = id; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setOwnerReply(String ownerReply) { this.ownerReply = ownerReply; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
}