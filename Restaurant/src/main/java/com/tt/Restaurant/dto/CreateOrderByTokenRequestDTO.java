package com.tt.Restaurant.dto;

import java.util.List;

public class CreateOrderByTokenRequestDTO {

    private String token;
    private String note;
    private String paymentOption;
    private String couponCode;
    private List<CreateOrderItemDTO> items;

    public CreateOrderByTokenRequestDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPaymentOption() {
        return paymentOption;
    }

    public void setPaymentOption(String paymentOption) {
        this.paymentOption = paymentOption;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {this.couponCode = couponCode;}

    public List<CreateOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemDTO> items) {
        this.items = items;
    }
}