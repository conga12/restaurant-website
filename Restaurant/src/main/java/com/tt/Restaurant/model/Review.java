package com.tt.Restaurant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User user;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "owner_reply", columnDefinition = "TEXT")
    private String ownerReply;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "coupon_discount_percent")
    private Integer couponDiscountPercent;

    @Column(name = "coupon_expires_at")
    private LocalDateTime couponExpiresAt;

    @Column(name = "coupon_used", nullable = false)
    private boolean couponUsed = false;

    @Column(name = "auto_reply_sent", nullable = false)
    private boolean autoReplySent = false;

    @Column(name = "auto_reply_sent_at")
    private LocalDateTime autoReplySentAt;

    @Column(name = "auto_reply_message", columnDefinition = "TEXT")
    private String autoReplyMessage;

    @Column(name = "ai_sentiment", length = 20)
    private String aiSentiment;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ReviewMedia> media = new ArrayList<>();

    public Review() {}

    public Long getId() { return id; }
    public Orders getOrder() { return order; }
    public Reservation getReservation() { return reservation; }
    public User getUser() { return user; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public String getOwnerReply() { return ownerReply; }
    public String getCouponCode() { return couponCode; }
    public Integer getCouponDiscountPercent() { return couponDiscountPercent; }
    public LocalDateTime getCouponExpiresAt() { return couponExpiresAt; }
    public boolean isCouponUsed() { return couponUsed; }
    public boolean isAutoReplySent() { return autoReplySent; }
    public LocalDateTime getAutoReplySentAt() { return autoReplySentAt; }
    public String getAutoReplyMessage() { return autoReplyMessage; }
    public String getAiSentiment() { return aiSentiment; }
    public String getAiSummary() { return aiSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ReviewMedia> getMedia() { return media; }

    public void setId(Long id) { this.id = id; }
    public void setOrder(Orders order) { this.order = order; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public void setUser(User user) { this.user = user; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setOwnerReply(String ownerReply) { this.ownerReply = ownerReply; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public void setCouponDiscountPercent(Integer couponDiscountPercent) { this.couponDiscountPercent = couponDiscountPercent; }
    public void setCouponExpiresAt(LocalDateTime couponExpiresAt) { this.couponExpiresAt = couponExpiresAt; }
    public void setCouponUsed(boolean couponUsed) { this.couponUsed = couponUsed; }
    public void setAutoReplySent(boolean autoReplySent) { this.autoReplySent = autoReplySent; }
    public void setAutoReplySentAt(LocalDateTime autoReplySentAt) { this.autoReplySentAt = autoReplySentAt; }
    public void setAutoReplyMessage(String autoReplyMessage) { this.autoReplyMessage = autoReplyMessage; }
    public void setAiSentiment(String aiSentiment) { this.aiSentiment = aiSentiment; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setMedia(List<ReviewMedia> media) { this.media = media; }
}