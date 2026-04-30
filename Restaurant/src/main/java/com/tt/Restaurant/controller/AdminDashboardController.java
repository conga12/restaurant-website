package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.AdminDashboardDTO;
import com.tt.Restaurant.model.*;
import com.tt.Restaurant.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/admin/api/dashboard")
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final RestaurantTableRepository tableRepository;
    private final OrderDetailRepository orderDetailRepository;

    public AdminDashboardController(
            OrderRepository orderRepository,
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            RestaurantTableRepository tableRepository,
            OrderDetailRepository orderDetailRepository
    ) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.tableRepository = tableRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    @GetMapping
    public AdminDashboardDTO dashboard() {
        AdminDashboardDTO dto = new AdminDashboardDTO();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<Orders> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<Reservation> reservations = reservationRepository.findAll();
        List<User> users = userRepository.findAll();
        List<RestaurantTable> tables = tableRepository.findAll();

        BigDecimal todayRevenue = revenueByDate(orders, today);
        BigDecimal yesterdayRevenue = revenueByDate(orders, yesterday);

        long todayOrders = ordersByDate(orders, today);
        long yesterdayOrders = ordersByDate(orders, yesterday);

        long todayReservations = reservationsByDate(reservations, today);
        long yesterdayReservations = reservationsByDate(reservations, yesterday);

        long totalCustomers = countCustomers(users);
        long yesterdayCustomers = countCustomersCreatedBeforeOrOn(users, yesterday);

        dto.todayRevenue = todayRevenue;
        dto.revenueChangePercent = percentChange(yesterdayRevenue.doubleValue(), todayRevenue.doubleValue());

        dto.totalOrders = orders.size();
        dto.orderChangePercent = percentChange(yesterdayOrders, todayOrders);

        dto.todayReservations = todayReservations;
        dto.reservationChangePercent = percentChange(yesterdayReservations, todayReservations);

        dto.totalCustomers = totalCustomers;
        dto.customerChangePercent = percentChange(yesterdayCustomers, totalCustomers);

        dto.revenueChart = buildWeeklyRevenue(orders);
        dto.orderStatus = buildOrderStatus(orders);
        dto.recentOrders = buildRecentOrders(orders);
        dto.todayReservationList = buildTodayReservations(reservations, today);
        dto.topDishes = buildTopDishes();
        dto.tables = buildTables(tables);

        return dto;
    }

    private BigDecimal revenueByDate(List<Orders> orders, LocalDate date) {
        return orders.stream()
                .filter(o -> o.getPaymentStatus() == Orders.PaymentStatus.PAID)
                .filter(o -> o.getPaidAt() != null && o.getPaidAt().toLocalDate().equals(date))
                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : o.getTotalAmount())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long ordersByDate(List<Orders> orders, LocalDate date) {
        return orders.stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().toLocalDate().equals(date))
                .count();
    }

    private long reservationsByDate(List<Reservation> reservations, LocalDate date) {
        return reservations.stream()
                .filter(r -> r.getReservationDate() != null && r.getReservationDate().equals(date))
                .count();
    }

    private long countCustomers(List<User> users) {
        return users.stream()
                .filter(u -> u.getRole() == User.Role.CUSTOMER)
                .count();
    }

    private long countCustomersCreatedBeforeOrOn(List<User> users, LocalDate date) {
        return users.stream()
                .filter(u -> u.getRole() == User.Role.CUSTOMER)
                .filter(u -> {
                    try {
                        return u.getCreatedAt() == null || !u.getCreatedAt().toLocalDate().isAfter(date);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .count();
    }

    private double percentChange(double oldValue, double newValue) {
        if (oldValue <= 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        return ((newValue - oldValue) / oldValue) * 100.0;
    }

    private List<BigDecimal> buildWeeklyRevenue(List<Orders> orders) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        List<BigDecimal> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            result.add(revenueByDate(orders, monday.plusDays(i)));
        }

        return result;
    }

    private Map<String, Long> buildOrderStatus(List<Orders> orders) {
        Map<String, Long> map = new HashMap<>();

        for (Orders o : orders) {
            String status = o.getStatus() != null ? o.getStatus().name() : "UNKNOWN";
            map.put(status, map.getOrDefault(status, 0L) + 1);
        }

        return map;
    }

    private List<Map<String, Object>> buildRecentOrders(List<Orders> orders) {
        return orders.stream()
                .limit(5)
                .map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", o.getId());
                    m.put("orderCode", o.getOrderCode());
                    m.put("tableNumber", o.getTable() != null ? o.getTable().getTableNumber() : "-");
                    m.put("amount", o.getFinalAmount() != null ? o.getFinalAmount() : o.getTotalAmount());
                    m.put("status", o.getStatus() != null ? o.getStatus().name() : "");
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> buildTodayReservations(List<Reservation> reservations, LocalDate today) {
        return reservations.stream()
                .filter(r -> r.getReservationDate() != null && r.getReservationDate().equals(today))
                .sorted(Comparator.comparing(
                        Reservation::getReservationTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .limit(5)
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("customerName", r.getCustomerName());
                    m.put("time", r.getReservationTime() != null ? r.getReservationTime().toString() : "");
                    m.put("guests", r.getNumberOfGuests());
                    m.put("status", r.getStatus() != null ? r.getStatus().name() : "");
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> buildTopDishes() {
        List<OrderDetail> details = orderDetailRepository.findAll();
        Map<String, Integer> sold = new HashMap<>();

        for (OrderDetail d : details) {
            if (d.getDish() == null) continue;
            String name = d.getDish().getName();
            int qty = d.getQuantity() == null ? 0 : d.getQuantity();
            sold.put(name, sold.getOrDefault(name, 0) + qty);
        }

        return sold.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("quantity", e.getValue());
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> buildTables(List<RestaurantTable> tables) {
        return tables.stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("tableNumber", t.getTableNumber());
                    m.put("capacity", t.getCapacity());
                    m.put("status", t.getStatus() != null ? t.getStatus().name() : "");
                    return m;
                })
                .toList();
    }
}