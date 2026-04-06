package com.tt.Restaurant.scheduler;

import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.Reservation.ReservationStatus;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import com.tt.Restaurant.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final EmailService emailService;

    public ReservationScheduler(ReservationRepository reservationRepository,
                                RestaurantTableRepository tableRepository,
                                EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.emailService = emailService;
    }

    // 1. Hủy booking đã CONFIRMED nhưng khách không đến sau 10 phút
    @Scheduled(fixedRate = 60000)
    public void autoCancelNoShowReservations() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> confirmedReservations = reservationRepository.findConfirmedReservationsToday(today);

        for (Reservation reservation : confirmedReservations) {
            LocalDateTime reservationDateTime = LocalDateTime.of(
                    reservation.getReservationDate(),
                    reservation.getReservationTime()
            );

            if (now.isAfter(reservationDateTime.plusMinutes(10))) {

                if (reservation.getDepositRequired() != null
                        && reservation.getDepositRequired()
                        && reservation.getDepositStatus() == Reservation.DepositStatus.PAID) {
                    reservation.setDepositStatus(Reservation.DepositStatus.FORFEITED);
                }

                reservation.setStatus(ReservationStatus.CANCELLED);
                reservation.setExpireAt(null);

                if (reservation.getTable() != null) {
                    RestaurantTable table = reservation.getTable();
                    table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
                    tableRepository.save(table);
                }

                reservationRepository.save(reservation);

                System.out.println("AUTO CANCEL NO-SHOW RESERVATION ID = " + reservation.getId());

                if (reservation.getCustomerEmail() != null && !reservation.getCustomerEmail().isBlank()) {
                    try {
                        emailService.sendReservationCancelledEmail(
                                reservation.getCustomerEmail(),
                                reservation.getCustomerName(),
                                reservation.getReservationDate().toString(),
                                reservation.getReservationTime().toString()
                        );
                    } catch (Exception e) {
                        System.out.println("GUI EMAIL HUY NO-SHOW THAT BAI: " + e.getMessage());
                    }
                }
            }
        }
    }

    // 2. Hủy booking PENDING nếu quá hạn thanh toán cọc
    @Scheduled(fixedRate = 60000)
    public void autoCancelExpiredPendingDepositReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> expiredPendingReservations =
                reservationRepository.findExpiredPendingReservations(now);

        for (Reservation reservation : expiredPendingReservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setDepositStatus(Reservation.DepositStatus.EXPIRED);
            reservation.setExpireAt(null);

            if (reservation.getTable() != null) {
                RestaurantTable table = reservation.getTable();
                table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
                tableRepository.save(table);
            }

            reservationRepository.save(reservation);

            System.out.println("AUTO CANCEL EXPIRED PAYMENT RESERVATION ID = " + reservation.getId());

            if (reservation.getCustomerEmail() != null && !reservation.getCustomerEmail().isBlank()) {
                try {
                    emailService.sendReservationExpiredEmail(
                            reservation.getCustomerEmail(),
                            reservation.getCustomerName(),
                            Math.toIntExact(reservation.getId()),
                            reservation.getReservationDate() != null ? reservation.getReservationDate().toString() : "",
                            reservation.getReservationTime() != null ? reservation.getReservationTime().toString() : "",
                            reservation.getDepositAmount()
                    );
                } catch (Exception e) {
                    System.out.println("GUI EMAIL HUY DO HET HAN THANH TOAN THAT BAI: " + e.getMessage());
                }
            }
        }
    }
}