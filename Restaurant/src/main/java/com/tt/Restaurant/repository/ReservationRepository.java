package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.Reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    List<Reservation> findByStatus(ReservationStatus status);

    long countByStatus(ReservationStatus status);

    long countBySeenFalse();

    @Modifying
    @Transactional
    @Query("update Reservation r set r.seen = true where r.seen = false")
    int markAllSeen();

    List<Reservation> findByReservationDate(LocalDate reservationDate);

    List<Reservation> findByUserId(Long userId);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    List<Reservation> findByUser_IdAndStatus(Long userId, Reservation.ReservationStatus status);

    @Query("""
        SELECT COUNT(r) > 0
        FROM Reservation r
        WHERE r.table.id = :tableId
          AND r.reservationDate = :reservationDate
          AND r.reservationTime = :reservationTime
          AND r.status IN ('PENDING', 'CONFIRMED')
    """)
    boolean existsConflict(Long tableId, LocalDate reservationDate, LocalTime reservationTime);

    @Query("""
        SELECT COUNT(r) > 0
        FROM Reservation r
        WHERE r.table.id = :tableId
          AND r.reservationDate = :reservationDate
          AND r.reservationTime = :reservationTime
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.id <> :reservationId
    """)
    boolean existsConflictForUpdate(Long reservationId, Long tableId, LocalDate reservationDate, LocalTime reservationTime);

    @Query("""
    SELECT r
    FROM Reservation r
    JOIN FETCH r.table t
    WHERE r.status = com.tt.Restaurant.model.Reservation.ReservationStatus.CONFIRMED
      AND r.reservationDate = :today
    """)
    List<Reservation> findConfirmedReservationsToday(LocalDate today);

    @Query("""
    SELECT r
    FROM Reservation r
    JOIN FETCH r.table t
    WHERE r.status = com.tt.Restaurant.model.Reservation.ReservationStatus.PENDING
      AND r.depositRequired = true
      AND r.depositStatus = com.tt.Restaurant.model.Reservation.DepositStatus.PENDING
      AND r.expireAt IS NOT NULL
      AND r.expireAt <= :now
    """)
    List<Reservation> findExpiredPendingReservations(LocalDateTime now);

    @Query("""
        select r.table.id
        from Reservation r
        where r.reservationDate = :date
          and r.reservationTime = :time
          and r.table is not null
          and r.status in :activeStatuses
    """)
    List<Long> findBookedTableIds(LocalDate date, LocalTime time, List<ReservationStatus> activeStatuses);
}