package com.tt.Restaurant.dto;

public class ReviewStatsDTO {
    private double averageRating;
    private long totalReviews;
    private int recommendedPercent;

    public ReviewStatsDTO() {}

    public ReviewStatsDTO(double averageRating, long totalReviews, int recommendedPercent) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.recommendedPercent = recommendedPercent;
    }

    public double getAverageRating() { return averageRating; }
    public long getTotalReviews() { return totalReviews; }
    public int getRecommendedPercent() { return recommendedPercent; }

    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }
    public void setRecommendedPercent(int recommendedPercent) { this.recommendedPercent = recommendedPercent; }
}