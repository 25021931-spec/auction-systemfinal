package com.auction.model.item;

import com.auction.enums.ItemCategory;

public class Art extends Item {

    private String artist;
    private int yearCreated;
    private String medium; // oil, watercolor, sculpture, etc.

    public Art() { super(); }

    public Art(String name, String description, double startingPrice,
               Long sellerId, String artist, int yearCreated, String medium) {
        super(name, description, startingPrice, ItemCategory.ART, sellerId);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.medium = medium;
    }

    @Override
    public String getInfo() {
        return String.format("Art[%s by %s (%d), Medium: %s, Starting: %.2f]",
                name, artist, yearCreated, medium, startingPrice);
    }

    @Override
    public String getCategoryDetails() {
        return String.format("Artist: %s | Year: %d | Medium: %s", artist, yearCreated, medium);
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getYearCreated() { return yearCreated; }
    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }
}
