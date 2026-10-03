package com.caleon.client.module;

public enum Category {
    COMBAT("Combat"), MISC("Misc"), BASEFINDING("Basefinding"), RENDER("Render"), DONUT("Donut");
    public final String label;
    Category(String label) { this.label = label; }
}
