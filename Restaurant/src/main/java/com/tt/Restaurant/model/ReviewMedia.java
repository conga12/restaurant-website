package com.tt.Restaurant.model;

import jakarta.persistence.*;

@Entity
@Table(name = "review_media")
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    public ReviewMedia() {}

    public ReviewMedia(Review review, String url) {
        this.review = review;
        this.url = url;
    }

    public Long getId() { return id; }
    public Review getReview() { return review; }
    public String getUrl() { return url; }

    public void setId(Long id) { this.id = id; }
    public void setReview(Review review) { this.review = review; }
    public void setUrl(String url) { this.url = url; }
}