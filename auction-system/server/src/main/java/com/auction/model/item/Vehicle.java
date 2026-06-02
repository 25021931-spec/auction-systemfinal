package com.auction.model.item;

import com.auction.enums.ItemCategory;

public class Vehicle extends Item {

    private String make;
    private String model;
    private int year;
    private int mileage;

    public Vehicle() { super(); }

    public Vehicle(String name, String description, double startingPrice,
                   Long sellerId, String make, String model, int year, int mileage) {
        super(name, description, startingPrice, ItemCategory.VEHICLE, sellerId);
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
    }

    @Override
    public String getInfo() {
        return String.format("Vehicle[%d %s %s, %d km, Starting: %.2f]",
                year, make, model, mileage, startingPrice);
    }

    @Override
    public String getCategoryDetails() {
        return String.format("Make: %s | Model: %s | Year: %d | Mileage: %d km",
                make, model, year, mileage);
    }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
}
