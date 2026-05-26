package com.example.aura_pc_app.domain.model;

public class ProductSpec {
    private String label;
    private String value;
    private int iconResId;

    public ProductSpec(String label, String value, int iconResId) {
        this.label = label;
        this.value = value;
        this.iconResId = iconResId;
    }

    public String getLabel() { return label; }
    public String getValue() { return value; }
    public int getIconResId() { return iconResId; }
}
