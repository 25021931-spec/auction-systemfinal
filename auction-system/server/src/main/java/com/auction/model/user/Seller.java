package com.auction.model.user;

import com.auction.enums.UserRole;

public class Seller extends User {

    private int totalItemsListed;
    private double totalRevenue;

    public Seller() { super(); }

    public Seller(String username, String passwordHash, String email) {
        super(username, passwordHash, email, UserRole.SELLER);
        this.totalItemsListed = 0;
        this.totalRevenue = 0.0;
    }

    @Override
    public boolean canBid() { return false; }

    @Override
    public boolean canSell() { return active; }

    @Override
    public boolean canManage() { return false; }

    @Override
    public String getInfo() {
        return String.format("Seller[%s, items=%d, revenue=%.2f]",
                username, totalItemsListed, totalRevenue);
    }

    public int getTotalItemsListed() { return totalItemsListed; }
    public void setTotalItemsListed(int totalItemsListed) { this.totalItemsListed = totalItemsListed; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public void addRevenue(double amount) { this.totalRevenue += amount; }
    public void incrementItemCount() { this.totalItemsListed++; }
}
