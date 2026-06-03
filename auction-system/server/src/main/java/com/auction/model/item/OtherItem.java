package com.auction.model.item;

import com.auction.enums.ItemCategory;

public class OtherItem extends Item {

    public OtherItem() { super(); }

    public OtherItem(String name, String description, double startingPrice, Long sellerId) {
        super(name, description, startingPrice, ItemCategory.OTHER, sellerId);
    }

    @Override
    public String getInfo() {
        return String.format("Item[%s, Starting: %.2f]", name, startingPrice);
    }

    @Override
    public String getCategoryDetails() {
        return "Category: Other";
    }
}
