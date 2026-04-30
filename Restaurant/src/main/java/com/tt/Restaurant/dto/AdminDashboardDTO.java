package com.tt.Restaurant.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDashboardDTO {
    public BigDecimal todayRevenue;
    public double revenueChangePercent;

    public long totalOrders;
    public double orderChangePercent;

    public long todayReservations;
    public double reservationChangePercent;

    public long totalCustomers;
    public double customerChangePercent;

    public List<BigDecimal> revenueChart;
    public Map<String, Long> orderStatus;

    public List<Map<String, Object>> recentOrders;
    public List<Map<String, Object>> todayReservationList;
    public List<Map<String, Object>> topDishes;
    public List<Map<String, Object>> tables;
}