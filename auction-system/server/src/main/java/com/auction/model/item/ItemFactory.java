package com.auction.model.item;

import com.auction.enums.ItemCategory;

/**
 * Factory Method Pattern: creates appropriate Item subclass based on category.
 */
public class ItemFactory {

    public static Item createItem(ItemCategory category, String name, String description,
                                   double startingPrice, Long sellerId) {
        return switch (category) {
            case ELECTRONICS -> new Electronics(name, description, startingPrice, sellerId,
                    "Unknown", "N/A", 0);
            case ART -> new Art(name, description, startingPrice, sellerId,
                    "Unknown", 2024, "Mixed");
            case VEHICLE -> new Vehicle(name, description, startingPrice, sellerId,
                    "Unknown", "Unknown", 2024, 0);
            default -> new OtherItem(name, description, startingPrice, sellerId);
        };
    }

    public static Item createElectronics(String name, String description, double startingPrice,
                                          Long sellerId, String brand, String modelNumber, int warranty) {
        return new Electronics(name, description, startingPrice, sellerId, brand, modelNumber, warranty);
    }

    public static Item createArt(String name, String description, double startingPrice,
                                  Long sellerId, String artist, int year, String medium) {
        return new Art(name, description, startingPrice, sellerId, artist, year, medium);
    }

    public static Item createVehicle(String name, String description, double startingPrice,
                                      Long sellerId, String make, String model, int year, int mileage) {
        return new Vehicle(name, description, startingPrice, sellerId, make, model, year, mileage);
    }
}
