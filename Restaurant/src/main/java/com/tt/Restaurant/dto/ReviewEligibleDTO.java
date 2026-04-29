package com.tt.Restaurant.dto;

import java.util.ArrayList;
import java.util.List;

public class ReviewEligibleDTO {

    public static class EligibleItem {
        private String value; // "order:123" hoặc "reservation:88"
        private String label;

        public EligibleItem() {}
        public EligibleItem(String value, String label) {
            this.value = value;
            this.label = label;
        }
        public String getValue() { return value; }
        public String getLabel() { return label; }
        public void setValue(String value) { this.value = value; }
        public void setLabel(String label) { this.label = label; }
    }

    private List<EligibleItem> items = new ArrayList<>();

    public ReviewEligibleDTO() {}

    public ReviewEligibleDTO(List<EligibleItem> items) {
        this.items = items;
    }

    public List<EligibleItem> getItems() { return items; }
    public void setItems(List<EligibleItem> items) { this.items = items; }
}