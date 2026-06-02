package com.auction.model.entity;

import java.time.LocalDateTime;

/**
 * Stores auto-bidding configuration for a specific bidder in an auction.
 * Used by the Auto-Bidding feature.
 */
public class AutoBidConfig {

    private Long bidderId;
    private String bidderName;
    private Long auctionId;
    private double maxBid;
    private double increment;
    private LocalDateTime registeredAt;

    public AutoBidConfig() {}

    public AutoBidConfig(Long bidderId, String bidderName, Long auctionId,
                          double maxBid, double increment) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }

    public Long getBidderId() { return bidderId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public double getMaxBid() { return maxBid; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }

    public double getIncrement() { return increment; }
    public void setIncrement(double increment) { this.increment = increment; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
