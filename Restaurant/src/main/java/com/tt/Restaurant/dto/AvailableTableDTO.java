package com.tt.Restaurant.dto;

public class AvailableTableDTO {
    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private String location;
    private String tableType;

    public AvailableTableDTO() {}

    public AvailableTableDTO(Long id, Integer tableNumber, Integer capacity, String location, String tableType) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.location = location;
        this.tableType = tableType;
    }

    public Long getId() { return id; }
    public Integer getTableNumber() { return tableNumber; }
    public Integer getCapacity() { return capacity; }
    public String getLocation() { return location; }
    public String getTableType() { return tableType; }

    public void setId(Long id) { this.id = id; }
    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setLocation(String location) { this.location = location; }
    public void setTableType(String tableType) { this.tableType = tableType; }
}