package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.dto.AuctionDto;
import com.auction.dto.BidDto;
import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemCategory;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AutoBidConfig;
import com.auction.model.entity.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Core auction business logic.
 * Uses per-auction ReentrantLock to prevent lost updates and race conditions
 * in concurrent bidding scenarios.
 */
public class AuctionService {

    private static final Logger logger = Logger.getLogger(AuctionService.class.getName());

    private final AuctionDao auctionDao;
    private final NotificationService notificationService;

    // Per-auction locks for concurrent bidding safety
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public AuctionService(NotificationService notificationService) {
        this.auctionDao = new AuctionDao();
        this.notificationService = notificationService;
    }

    private ReentrantLock getLock(Long auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock(true));
    }

    // -------- Auction CRUD --------

    public AuctionDto createAuction(AuctionDto dto) throws AuctionException {
        try {
            Item item = ItemFactory.createItem(
                    dto.getCategory() != null ? dto.getCategory() : ItemCategory.OTHER,
                    dto.getItemName(),
                    dto.getItemDescription(),
                    dto.getStartingPrice(),
                    dto.getSellerId()
            );

            LocalDateTime startTime = dto.getStartTime() != null ? dto.getStartTime() : LocalDateTime.now();
            LocalDateTime endTime = dto.getEndTime();
            if (endTime == null || !endTime.isAfter(startTime)) {
                throw new AuctionException("End time must be after start time");
            }

            Auction auction = new Auction(item, dto.getSellerId(), startTime, endTime);
            auctionDao.saveAuction(auction);
            logger.info("Created auction id=" + auction.getId());
            return toDto(auction, null);
        } catch (SQLException e) {
            throw new AuctionException("Failed to create auction: " + e.getMessage(), e);
        }
    }

    public List<AuctionDto> getAllAuctions() throws AuctionException {
        try {
            List<Auction> auctions = auctionDao.findAll();
            // Update statuses based on current time
            for (Auction a : auctions) {
                AuctionStatus prev = a.getStatus();
                a.updateStatus();
                if (prev != a.getStatus()) {
                    auctionDao.updateAuction(a);
                }
            }
            return auctions.stream().map(a -> toDto(a, null)).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new AuctionException("Failed to fetch auctions", e);
        }
    }

    public AuctionDto getAuctionDetail(Long auctionId) throws AuctionException {
        try {
            Auction auction = getAuctionOrThrow(auctionId);
            auction.updateStatus();
            auctionDao.updateAuction(auction);
            return toDto(auction, auction.getBidHistory());
        } catch (SQLException e) {
            throw new AuctionException("Failed to fetch auction detail", e);
        }
    }

    // -------- Bidding --------

    /**
     * Place a manual bid. Thread-safe per auction via ReentrantLock.
     */
    public AuctionDto placeBid(BidDto bidDto) throws AuctionException {
        ReentrantLock lock = getLock(bidDto.getAuctionId());
        lock.lock();
        try {
            Auction auction = getAuctionOrThrow(bidDto.getAuctionId());
            auction.updateStatus();

            validateBid(auction, bidDto.getBidderId(), bidDto.getAmount());

            // Apply the bid
            double newPrice = bidDto.getAmount();
            BidTransaction bid = new BidTransaction(
                    auction.getId(), bidDto.getBidderId(),
                    bidDto.getBidderName(), newPrice, false
            );
            auctionDao.saveBid(bid);

            auction.setCurrentPrice(newPrice);
            auction.setCurrentLeaderId(bidDto.getBidderId());
            auction.setCurrentLeaderName(bidDto.getBidderName());

            // Anti-sniping check
            boolean extended = auction.checkAndExtend();
            auctionDao.updateAuction(auction);

            AuctionDto result = toDto(auction, null);
            result.setTotalBids(auction.getBidHistory().size() + 1);

            // Notify all observers (other clients watching this auction)
            notificationService.notifyBidUpdate(result, extended);

            // Trigger auto-bids from other bidders
            processAutoBids(auction, bidDto.getBidderId(), newPrice);

            return result;
        } catch (SQLException e) {
            throw new AuctionException("Database error during bid: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Register auto-bid configuration.
     */
    public void setAutoBid(BidDto dto) throws AuctionException {
        try {
            Auction auction = getAuctionOrThrow(dto.getAuctionId());
            if (auction.isFinished()) {
                throw new AuctionException("Auction has ended.");
            }
            if (dto.getMaxBid() <= auction.getCurrentPrice()) {
                throw new AuctionException("Max bid must be greater than current price: " + auction.getCurrentPrice());
            }
            AutoBidConfig config = new AutoBidConfig(
                    dto.getBidderId(), dto.getBidderName(),
                    dto.getAuctionId(), dto.getMaxBid(), dto.getIncrement()
            );
            auctionDao.saveAutoBidConfig(config);
            logger.info("Auto-bid registered for bidder " + dto.getBidderName());
        } catch (SQLException e) {
            throw new AuctionException("Failed to save auto-bid config", e);
        }
    }

    /**
     * Process auto-bids after a manual bid is placed.
     * Competing auto-bidders respond in registration order.
     */
    private void processAutoBids(Auction auction, Long triggerBidderId, double currentPrice)
            throws SQLException, AuctionException {
        List<AutoBidConfig> configs = auctionDao.findAutoBidConfigs(auction.getId());

        // Find the auto-bid with highest maxBid that isn't the trigger bidder
        AutoBidConfig winner = null;
        for (AutoBidConfig c : configs) {
            if (!c.getBidderId().equals(triggerBidderId) && c.getMaxBid() > currentPrice) {
                if (winner == null || c.getMaxBid() > winner.getMaxBid() ||
                        (c.getMaxBid() == winner.getMaxBid() &&
                                c.getRegisteredAt().isBefore(winner.getRegisteredAt()))) {
                    winner = c;
                }
            }
        }

        if (winner == null) return;

        double nextBid = currentPrice + winner.getIncrement();
        if (nextBid > winner.getMaxBid()) nextBid = winner.getMaxBid();
        if (nextBid <= currentPrice) return;

        BidTransaction autoBid = new BidTransaction(
                auction.getId(), winner.getBidderId(), winner.getBidderName(), nextBid, true
        );
        auctionDao.saveBid(autoBid);
        auction.setCurrentPrice(nextBid);
        auction.setCurrentLeaderId(winner.getBidderId());
        auction.setCurrentLeaderName(winner.getBidderName());
        boolean extended = auction.checkAndExtend();
        auctionDao.updateAuction(auction);

        AuctionDto result = toDto(auction, null);
        notificationService.notifyBidUpdate(result, extended);
        logger.info("Auto-bid placed: " + winner.getBidderName() + " -> " + nextBid);
    }

    // -------- Session end --------

    /**
     * Called periodically to finish expired auctions.
     */
    public void processExpiredAuctions() {
        try {
            List<Auction> running = auctionDao.findByStatus(AuctionStatus.RUNNING);
            for (Auction a : running) {
                a.updateStatus();
                if (a.getStatus() == AuctionStatus.FINISHED) {
                    auctionDao.updateAuction(a);
                    notificationService.notifyAuctionEnded(toDto(a, null));
                    logger.info("Auction " + a.getId() + " finished. Winner: " + a.getCurrentLeaderName());
                }
            }
            // Activate OPEN auctions that have passed start time
            List<Auction> open = auctionDao.findByStatus(AuctionStatus.OPEN);
            for (Auction a : open) {
                a.updateStatus();
                if (a.getStatus() == AuctionStatus.RUNNING) {
                    auctionDao.updateAuction(a);
                }
            }
        } catch (SQLException e) {
            logger.warning("Error processing expired auctions: " + e.getMessage());
        }
    }

    // -------- Validation --------

    private void validateBid(Auction auction, Long bidderId, double amount) throws AuctionException {
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionException("Auction is not currently running. Status: " + auction.getStatus());
        }
        if (amount <= auction.getCurrentPrice()) {
            throw new AuctionException(String.format(
                    "Bid %.2f must be higher than current price %.2f", amount, auction.getCurrentPrice()));
        }
        if (bidderId != null && bidderId.equals(auction.getCurrentLeaderId())) {
            throw new AuctionException("You are already the highest bidder.");
        }
    }

    private Auction getAuctionOrThrow(Long id) throws SQLException, AuctionException {
        return auctionDao.findById(id).orElseThrow(
                () -> new AuctionException("Auction not found: " + id));
    }

    // -------- DTO mapping --------

    public AuctionDto toDto(Auction auction, List<BidTransaction> bids) {
        AuctionDto dto = new AuctionDto();
        dto.setId(auction.getId());
        dto.setSellerId(auction.getSellerId());
        dto.setCurrentPrice(auction.getCurrentPrice());
        dto.setCurrentLeader(auction.getCurrentLeaderName());
        dto.setStartTime(auction.getStartTime());
        dto.setEndTime(auction.getEndTime());
        dto.setStatus(auction.getStatus());

        if (auction.getItem() != null) {
            Item item = auction.getItem();
            dto.setItemName(item.getName());
            dto.setItemDescription(item.getDescription());
            dto.setStartingPrice(item.getStartingPrice());
            dto.setCategory(item.getCategory());
        }

        if (bids != null) {
            dto.setBidHistory(bids.stream().map(this::bidToDto).collect(Collectors.toList()));
            dto.setTotalBids(bids.size());
        }
        return dto;
    }

    private BidDto bidToDto(BidTransaction bid) {
        BidDto dto = new BidDto();
        dto.setId(bid.getId());
        dto.setAuctionId(bid.getAuctionId());
        dto.setBidderId(bid.getBidderId());
        dto.setBidderName(bid.getBidderName());
        dto.setAmount(bid.getAmount());
        dto.setBidTime(bid.getBidTime());
        dto.setAutoBid(bid.isAutoBid());
        return dto;
    }
}
