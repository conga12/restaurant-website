package com.tt.Restaurant.dto;

import java.util.List;

public class CreateOrderRequestDTO {

    private Integer reservationId;
    private String customerPhone;
    private String note;
    private String paymentOption;
    private List<CreateOrderItemDTO> items;

    public CreateOrderRequestDTO() {
    }

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
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

    public List<CreateOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemDTO> items) {
        this.items = items;
    }
}