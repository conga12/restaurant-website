package com.tt.Restaurant.service;

import org.springframework.mail.SimpleMailMessage;

import java.math.BigDecimal;

public interface EmailService {

    void sendReservationConfirmedEmail(
            String to,
            String customerName,
            String date,
            String time,
            Integer guests,
            Integer reservationId,
            String customerPhone
    );

    // ✅ NEW: magic link 30 phút
    void sendReservationConfirmedEmailWithOrderLink(
            String to,
            String customerName,
            String date,
            String time,
            Integer guests,
            Integer reservationId,
            String customerPhone,
            String orderLink
    );

    void sendReservationCancelledEmail(
            String to,
            String customerName,
            String date,
            String time
    );

    void sendDepositPaidEmail(
            String to,
            String customerName,
            Integer reservationId,
            String date,
            String time,
            BigDecimal depositAmount
    );

    // ✅ NEW: magic link 30 phút cho mail cọc
    void sendDepositPaidEmailWithOrderLink(
            String to,
            String customerName,
            Integer reservationId,
            String date,
            String time,
            BigDecimal depositAmount,
            String orderLink
    );

    void sendDepositRefundedEmail(
            String to,
            String customerName,
            Integer reservationId,
            BigDecimal depositAmount
    );

    void sendReservationExpiredEmail(String toEmail,
                                     String customerName,
                                     Integer reservationId,
                                     String reservationDate,
                                     String reservationTime,
                                     java.math.BigDecimal depositAmount);

    void sendPasswordResetEmail(String to, String resetLink);

    void sendDepositRequestEmail(String email, String name, String paymentUrl, String date, String time);

    void sendLowRatingReviewAutoReply(String to, String customerName, String message);
}