package com.example.ambicare;

public class SettingItem {
    private int icon;
    private String name;

    public SettingItem(int icon, String name) {
        this.icon = icon;
        this.name = name;
    }

    public int getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }
}
