package com.auction.model.user;

import com.auction.enums.UserRole;

public class Admin extends User {

    public Admin() { super(); }

    public Admin(String username, String passwordHash, String email) {
        super(username, passwordHash, email, UserRole.ADMIN);
    }

    @Override
    public boolean canBid() { return false; }

    @Override
    public boolean canSell() { return false; }

    @Override
    public boolean canManage() { return true; }

    @Override
    public String getInfo() {
        return String.format("Admin[%s]", username);
    }
}
