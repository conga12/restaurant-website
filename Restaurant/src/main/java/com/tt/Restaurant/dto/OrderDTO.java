package com.tt.Restaurant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDTO {

    private Integer id;
    private String customerName;
    private Integer tableNumber;
    private Integer totalItems;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;

    // getters setters
}