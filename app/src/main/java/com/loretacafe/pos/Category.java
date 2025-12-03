package com.loretacafe.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Category implements Serializable {
    private String id;
    private String name;
    private String iconName; // Name of the icon drawable
    private int itemCount;
    private List<MenuItem> items;

    public Category(String id, String name, String iconName) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
        this.itemCount = 0;
        this.items = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
    }

    public void addItem(MenuItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public void removeItem(MenuItem item) {
        if (this.items != null) {
            this.items.remove(item);
        }
    }
}

