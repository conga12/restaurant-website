package com.tt.Restaurant.service;

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
}