package com.auction.model.user;

import com.auction.enums.UserRole;
import com.auction.model.entity.Entity;

/**
 * Abstract User class - Bidder, Seller, Admin extend this.
 * Demonstrates Inheritance and Abstraction.
 */
public abstract class User extends Entity {

    protected String username;
    protected String passwordHash;
    protected String email;
    protected UserRole role;
    protected boolean active;

    protected User() { super(); }

    protected User(String username, String passwordHash, String email, UserRole role) {
        super();
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
        this.active = true;
    }

    @Override
    public String getInfo() {
        return String.format("User[%s, role=%s, active=%b]", username, role, active);
    }

    public abstract boolean canBid();
    public abstract boolean canSell();
    public abstract boolean canManage();

    // Getters & Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
