package com.auction.model.entity;

import java.time.LocalDateTime;

/**
 * Represents a single bid transaction in an auction.
 */
public class BidTransaction extends Entity {

    private Long auctionId;
    private Long bidderId;
    private String bidderName;
    private double amount;
    private LocalDateTime bidTime;
    private boolean isAutoBid;

    public BidTransaction() { super(); }

    public BidTransaction(Long auctionId, Long bidderId, String bidderName,
                           double amount, boolean isAutoBid) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();
        this.isAutoBid = isAutoBid;
    }

    @Override
    public String getInfo() {
        return String.format("Bid[auction=%d, bidder=%s, amount=%.2f, time=%s, auto=%b]",
                auctionId, bidderName, amount, bidTime, isAutoBid);
    }

    // Getters & Setters
    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public Long getBidderId() { return bidderId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public boolean isAutoBid() { return isAutoBid; }
    public void setAutoBid(boolean autoBid) { isAutoBid = autoBid; }
}
