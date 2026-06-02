package com.auction.model.user;

import com.auction.enums.UserRole;

public class Bidder extends User {

    private double balance;
    private int totalBidsPlaced;

    public Bidder() { super(); }

    public Bidder(String username, String passwordHash, String email) {
        super(username, passwordHash, email, UserRole.BIDDER);
        this.balance = 0.0;
        this.totalBidsPlaced = 0;
    }

    @Override
    public boolean canBid() { return active; }

    @Override
    public boolean canSell() { return false; }

    @Override
    public boolean canManage() { return false; }

    @Override
    public String getInfo() {
        return String.format("Bidder[%s, bids=%d]", username, totalBidsPlaced);
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public int getTotalBidsPlaced() { return totalBidsPlaced; }
    public void setTotalBidsPlaced(int totalBidsPlaced) { this.totalBidsPlaced = totalBidsPlaced; }

    public void incrementBidCount() { this.totalBidsPlaced++; }
}
