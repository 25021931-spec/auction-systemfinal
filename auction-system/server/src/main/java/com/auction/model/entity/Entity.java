package com.auction.model.entity;

import java.time.LocalDateTime;

/**
 * Abstract base entity following OOP Abstraction principle.
 * All domain objects inherit from this.
 */
public abstract class Entity {

    protected Long id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    protected Entity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    protected Entity(Long id) {
        this();
        this.id = id;
    }

    public abstract String getInfo();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return getInfo();
    }
}
