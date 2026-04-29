package com.tt.Restaurant.dto;

public class ReviewAlertDTO {
    private String type;   // LOW_RATING_REVIEW
    private Long reviewId;
    private Integer rating;
    private String comment;

    public ReviewAlertDTO() {}

    public ReviewAlertDTO(String type, Long reviewId, Integer rating, String comment) {
        this.type = type;
        this.reviewId = reviewId;
        this.rating = rating;
        this.comment = comment;
    }

    public String getType() { return type; }
    public Long getReviewId() { return reviewId; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }

    public void setType(String type) { this.type = type; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
}