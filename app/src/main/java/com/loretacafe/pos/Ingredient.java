package com.loretacafe.pos;

import java.io.Serializable;

public class Ingredient implements Serializable {
    private String id;
    private String name;
    private double costPerUnit;
    private String unit;

    public Ingredient(String id, String name, double costPerUnit, String unit) {
        this.id = id;
        this.name = name;
        this.costPerUnit = costPerUnit;
        this.unit = unit;
    }

    // Getters and Setters
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

    public double getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(double costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return name;
    }
}

