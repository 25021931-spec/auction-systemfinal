package com.auction.model.item;

import com.auction.enums.ItemCategory;

public class Electronics extends Item {

    private String brand;
    private String modelNumber;
    private int warrantyMonths;

    public Electronics() { super(); }

    public Electronics(String name, String description, double startingPrice,
                       Long sellerId, String brand, String modelNumber, int warrantyMonths) {
        super(name, description, startingPrice, ItemCategory.ELECTRONICS, sellerId);
        this.brand = brand;
        this.modelNumber = modelNumber;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getInfo() {
        return String.format("Electronics[%s - %s %s, Warranty: %d months, Starting: %.2f]",
                name, brand, modelNumber, warrantyMonths, startingPrice);
    }

    @Override
    public String getCategoryDetails() {
        return String.format("Brand: %s | Model: %s | Warranty: %d months", brand, modelNumber, warrantyMonths);
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}
