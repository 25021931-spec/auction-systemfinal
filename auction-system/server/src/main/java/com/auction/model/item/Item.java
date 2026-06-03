package com.auction.model.item;

import com.auction.enums.ItemCategory;
import com.auction.model.entity.Entity;

/**
 * Abstract Item class - inherits Entity.
 * Specific item types (Electronics, Art, Vehicle) extend this.
 */
public abstract class Item extends Entity {

    protected String name;
    protected String description;
    protected double startingPrice;
    protected ItemCategory category;
    protected Long sellerId;

    protected Item() { super(); }

    protected Item(String name, String description, double startingPrice,
                   ItemCategory category, Long sellerId) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.category = category;
        this.sellerId = sellerId;
    }

    // Polymorphism: subclasses override to provide specific info
    @Override
    public abstract String getInfo();

    public abstract String getCategoryDetails();

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
}
