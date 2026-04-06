package com.tt.Restaurant.dto;

public class VerifyReservationRequestDTO {

    private Integer reservationId;
    private String customerPhone;

    public VerifyReservationRequestDTO() {
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
}