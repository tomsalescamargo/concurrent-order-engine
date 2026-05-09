package com.swishsales.concurrent.entity;

public class ItemBasketball extends Item {

    private Integer size;
    private String brand;
    private String model;

    public ItemBasketball(String id, String title, String description, Integer size, String brand, String model) {
        super(id, title, description);
        this.size = size;
        this.brand = brand;
        this.model = model;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
