package com.auction.model.entity;

import com.auction.enums.AuctionStatus;
import com.auction.model.item.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Central Auction entity managing the full lifecycle of an auction.
 * Status transitions: OPEN → RUNNING → FINISHED → PAID / CANCELED
 */
public class Auction extends Entity {

    private Item item;
    private Long sellerId;
    private double currentPrice;
    private Long currentLeaderId;
    private String currentLeaderName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory;

    // Anti-sniping config
    private static final int SNIPE_WINDOW_SECONDS = 30;
    private static final int EXTENSION_SECONDS = 60;

    public Auction() {
        super();
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
    }

    public Auction(Item item, Long sellerId, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.item = item;
        this.sellerId = sellerId;
        this.currentPrice = item.getStartingPrice();
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Check and update status based on current time.
     */
    public void updateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (status == AuctionStatus.OPEN && !now.isBefore(startTime)) {
            status = AuctionStatus.RUNNING;
        }
        if (status == AuctionStatus.RUNNING && !now.isBefore(endTime)) {
            status = AuctionStatus.FINISHED;
        }
    }

    /**
     * Anti-sniping: extend end time if bid placed in last SNIPE_WINDOW_SECONDS.
     */
    public boolean checkAndExtend() {
        LocalDateTime now = LocalDateTime.now();
        long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();
        if (secondsLeft > 0 && secondsLeft <= SNIPE_WINDOW_SECONDS) {
            endTime = endTime.plusSeconds(EXTENSION_SECONDS);
            return true;
        }
        return false;
    }

    @Override
    public String getInfo() {
        return String.format("Auction[id=%d, item=%s, price=%.2f, status=%s, ends=%s]",
                id, item != null ? item.getName() : "N/A", currentPrice, status, endTime);
    }

    public boolean isActive() {
        return status == AuctionStatus.RUNNING;
    }

    public boolean isFinished() {
        return status == AuctionStatus.FINISHED
                || status == AuctionStatus.PAID
                || status == AuctionStatus.CANCELED;
    }

    // Getters & Setters
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public Long getCurrentLeaderId() { return currentLeaderId; }
    public void setCurrentLeaderId(Long currentLeaderId) { this.currentLeaderId = currentLeaderId; }

    public String getCurrentLeaderName() { return currentLeaderName; }
    public void setCurrentLeaderName(String currentLeaderName) { this.currentLeaderName = currentLeaderName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<BidTransaction> bidHistory) { this.bidHistory = bidHistory; }
}
