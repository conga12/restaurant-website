package com.tt.Restaurant.dto;

import java.util.List;

public class CreateQrOrderRequestDTO {

    private Long tableId;
    private String note;
    private List<CreateOrderItemDTO> items;

    public CreateQrOrderRequestDTO() {
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<CreateOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemDTO> items) {
        this.items = items;
    }
}