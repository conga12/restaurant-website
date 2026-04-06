package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.model.Orders;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.Reservation.ReservationStatus;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.OrderRepository;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import com.tt.Restaurant.repository.UserRepository;
import com.tt.Restaurant.service.EmailService;
import com.tt.Restaurant.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            RestaurantTableRepository tableRepository,
            EmailService emailService,
            UserRepository userRepository,
            OrderRepository orderRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));
    }

    @Override
    public List<Reservation> getReservationsOfCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        return reservationRepository.findByUserId(user.getId());
    }

    @Override
    public Reservation getReservationByIdForCurrentUser(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getUser() == null || !reservation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xem đặt bàn này");
        }

        return reservation;
    }

    @Override
    public Reservation createReservation(Reservation reservation) {
        validateReservationInput(reservation);

        if (reservation.getReservationDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Không thể đặt bàn trong quá khứ");
        }

        if (reservation.getTable() == null || reservation.getTable().getId() == null) {
            throw new RuntimeException("Vui lòng chọn bàn");
        }

        RestaurantTable table = tableRepository.findById(reservation.getTable().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));

        if (table.getCapacity() < reservation.getNumberOfGuests()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bàn không đủ sức chứa"
            );
        }

        boolean conflict = reservationRepository.existsConflict(
                table.getId(),
                reservation.getReservationDate(),
                reservation.getReservationTime()
        );

        if (conflict) {
            throw new RuntimeException("Bàn đã được đặt ở khung giờ này");
        }

        reservation.setTable(table);

        if (table.getTableType() == RestaurantTable.TableType.VIP) {
            reservation.setDepositRequired(true);
            reservation.setDepositAmount(new java.math.BigDecimal("300000"));
            reservation.setDepositStatus(Reservation.DepositStatus.PENDING);
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setExpireAt(java.time.LocalDateTime.now().plusMinutes(15));

        } else if (table.getTableType() == RestaurantTable.TableType.PRIVATE_ROOM) {
            reservation.setDepositRequired(true);
            reservation.setDepositAmount(new java.math.BigDecimal("500000"));
            reservation.setDepositStatus(Reservation.DepositStatus.PENDING);
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setExpireAt(java.time.LocalDateTime.now().plusMinutes(15));

        } else {
            reservation.setDepositRequired(false);
            reservation.setDepositAmount(java.math.BigDecimal.ZERO);
            reservation.setDepositStatus(Reservation.DepositStatus.NOT_REQUIRED);
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setExpireAt(null);

            table.setStatus(RestaurantTable.TableStatus.RESERVED);
            tableRepository.save(table);
        }

        Reservation saved = reservationRepository.save(reservation);

        if (saved.getStatus() == ReservationStatus.CONFIRMED) {
            sendConfirmedMailIfPossible(saved);
        }

        return saved;
    }

    @Override
    public Reservation createReservationForCurrentUser(Reservation reservation, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        if (user.getRole() != User.Role.CUSTOMER) {
            throw new RuntimeException("Chỉ customer mới được đặt bàn");
        }

        if (reservation.getCustomerName() == null || reservation.getCustomerName().isBlank()) {
            throw new RuntimeException("Tên khách hàng không được để trống");
        }

        if (reservation.getCustomerPhone() == null || reservation.getCustomerPhone().isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        if (reservation.getCustomerEmail() == null || reservation.getCustomerEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        reservation.setUser(user);

        return createReservation(reservation);
    }

    @Override
    public Reservation updateReservation(Long id, Reservation reservation) {
        Reservation existing = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        validateReservationInput(reservation);

        if (reservation.getReservationDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Không thể cập nhật sang ngày trong quá khứ");
        }

        RestaurantTable table = null;
        if (reservation.getTable() != null && reservation.getTable().getId() != null) {
            table = tableRepository.findById(reservation.getTable().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));

            if (table.getCapacity() < reservation.getNumberOfGuests()) {
                throw new RuntimeException("Bàn không đủ sức chứa");
            }

            boolean conflict = reservationRepository.existsConflictForUpdate(
                    id,
                    table.getId(),
                    reservation.getReservationDate(),
                    reservation.getReservationTime()
            );

            if (conflict) {
                throw new RuntimeException("Bàn đã được đặt ở khung giờ này");
            }
        }

        existing.setCustomerName(reservation.getCustomerName());
        existing.setCustomerPhone(reservation.getCustomerPhone());
        existing.setCustomerEmail(reservation.getCustomerEmail());
        existing.setReservationDate(reservation.getReservationDate());
        existing.setReservationTime(reservation.getReservationTime());
        existing.setNumberOfGuests(reservation.getNumberOfGuests());
        existing.setSpecialRequest(reservation.getSpecialRequest());

        if (table != null) {
            existing.setTable(table);
        }

        return reservationRepository.save(existing);
    }

    @Override
    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Đặt bàn đã bị hủy");
        }

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new RuntimeException("Đặt bàn đã hoàn tất");
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return reservation;
        }

        if (reservation.getTable() == null || reservation.getTable().getId() == null) {
            throw new RuntimeException("Cần chọn bàn trước khi xác nhận");
        }

        RestaurantTable table = tableRepository.findById(reservation.getTable().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));

        if (table.getCapacity() < reservation.getNumberOfGuests()) {
            throw new RuntimeException("Bàn không đủ sức chứa");
        }

        boolean conflict = reservationRepository.existsConflictForUpdate(
                reservation.getId(),
                table.getId(),
                reservation.getReservationDate(),
                reservation.getReservationTime()
        );

        if (conflict) {
            throw new RuntimeException("Bàn đã được đặt ở khung giờ này");
        }

        if (reservation.getExpireAt() != null &&
                reservation.getExpireAt().isBefore(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đặt bàn đã hết hạn thanh toán"
            );
        }

        if (reservation.getDepositRequired() != null && reservation.getDepositRequired()) {
            if (reservation.getDepositStatus() != Reservation.DepositStatus.PAID) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đặt bàn này cần cọc trước khi xác nhận"
                );
            }
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpireAt(null);

        table.setStatus(RestaurantTable.TableStatus.RESERVED);
        tableRepository.save(table);

        Reservation saved = reservationRepository.save(reservation);
        sendConfirmedMailIfPossible(saved);

        return saved;
    }

    @Override
    public Reservation completeReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Đặt bàn đã bị hủy");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setExpireAt(null);

        if (reservation.getTable() != null) {
            RestaurantTable table = reservation.getTable();
            table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return reservation;
        }

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new RuntimeException("Đặt bàn đã hoàn tất, không thể hủy");
        }

        orderRepository.findByReservationId(id).ifPresent(order -> {
            if (order.getStatus() == Orders.OrderStatus.PENDING
                    || order.getStatus() == Orders.OrderStatus.PREPARING) {
                order.setStatus(Orders.OrderStatus.CANCELLED);
                order.setUpdatedAt(java.time.LocalDateTime.now());
                orderRepository.save(order);
            }
        });

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setExpireAt(null);

        if (reservation.getTable() != null) {
            RestaurantTable table = reservation.getTable();
            table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation cancelReservationForCurrentUser(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getUser() == null || !reservation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đặt bàn này");
        }

        return cancelReservation(id);
    }

    @Override
    public List<RestaurantTable> getAvailableTables(LocalDate date, LocalTime time, Integer guests) {
        if (date == null || time == null || guests == null || guests <= 0) {
            throw new RuntimeException("Dữ liệu tìm bàn không hợp lệ");
        }

        List<RestaurantTable> suitableTables = tableRepository.findByCapacityGreaterThanEqual(guests);

        return suitableTables.stream()
                .filter(table -> table.getStatus() == RestaurantTable.TableStatus.AVAILABLE)
                .filter(table -> !reservationRepository.existsConflict(table.getId(), date, time))
                .toList();
    }

    private void validateReservationInput(Reservation reservation) {
        if (reservation.getCustomerName() == null || reservation.getCustomerName().isBlank()) {
            throw new RuntimeException("Tên khách hàng không được để trống");
        }

        if (reservation.getCustomerPhone() == null || reservation.getCustomerPhone().isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        if (reservation.getReservationDate() == null) {
            throw new RuntimeException("Ngày đặt bàn không được để trống");
        }

        if (reservation.getReservationTime() == null) {
            throw new RuntimeException("Giờ đặt bàn không được để trống");
        }

        if (reservation.getNumberOfGuests() == null || reservation.getNumberOfGuests() <= 0) {
            throw new RuntimeException("Số lượng khách không hợp lệ");
        }
    }

    private void sendConfirmedMailIfPossible(Reservation reservation) {
        if (reservation.getCustomerEmail() != null && !reservation.getCustomerEmail().isBlank()) {
            emailService.sendReservationConfirmedEmail(
                    reservation.getCustomerEmail(),
                    reservation.getCustomerName(),
                    reservation.getReservationDate() != null ? reservation.getReservationDate().toString() : "",
                    reservation.getReservationTime() != null ? reservation.getReservationTime().toString() : "",
                    reservation.getNumberOfGuests(),
                    Math.toIntExact(reservation.getId()),
                    reservation.getCustomerPhone()
            );
        }
    }
}