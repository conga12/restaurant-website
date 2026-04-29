package com.tt.Restaurant.dto;

public class ReviewRequestDTO {
    private Long orderId;
    private Long reservationId;
    private Integer rating;
    private String comment;
    private java.util.List<String> mediaUrls;

    public Long getOrderId() { return orderId; }
    public Long getReservationId() { return reservationId; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public java.util.List<String> getMediaUrls() { return mediaUrls; }

    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setMediaUrls(java.util.List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
}