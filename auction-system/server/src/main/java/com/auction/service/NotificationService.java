package com.auction.service;

import com.auction.dto.AuctionDto;
import com.auction.dto.SocketMessage;
import com.auction.enums.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Observer Pattern: manages subscriptions and pushes real-time updates
 * to all clients watching a specific auction.
 * Thread-safe via ConcurrentHashMap and synchronized writer access.
 */
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    // auctionId -> list of client writers subscribed
    private final ConcurrentHashMap<Long, List<PrintWriter>> subscribers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Subscribe a client to auction updates.
     */
    public void subscribe(Long auctionId, PrintWriter writer) {
        subscribers.computeIfAbsent(auctionId, id -> Collections.synchronizedList(new ArrayList<>()))
                .add(writer);
        logger.fine("Client subscribed to auction " + auctionId);
    }

    /**
     * Unsubscribe a client from all auctions.
     */
    public void unsubscribeAll(PrintWriter writer) {
        subscribers.values().forEach(list -> list.remove(writer));
    }

    /**
     * Unsubscribe client from specific auction.
     */
    public void unsubscribe(Long auctionId, PrintWriter writer) {
        List<PrintWriter> list = subscribers.get(auctionId);
        if (list != null) list.remove(writer);
    }

    /**
     * Push bid update to all subscribers of an auction.
     */
    public void notifyBidUpdate(AuctionDto auction, boolean extended) {
        SocketMessage msg = new SocketMessage(
                extended ? MessageType.AUCTION_EXTENDED : MessageType.BID_UPDATE,
                auction
        );
        broadcast(auction.getId(), msg);
    }

    /**
     * Push auction ended notification.
     */
    public void notifyAuctionEnded(AuctionDto auction) {
        SocketMessage msg = new SocketMessage(MessageType.AUCTION_ENDED, auction);
        broadcast(auction.getId(), msg);
    }

    private void broadcast(Long auctionId, SocketMessage message) {
        List<PrintWriter> writers = subscribers.get(auctionId);
        if (writers == null || writers.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.warning("Failed to serialize notification: " + e.getMessage());
            return;
        }

        List<PrintWriter> dead = new ArrayList<>();
        synchronized (writers) {
            for (PrintWriter writer : writers) {
                try {
                    writer.println(json);
                    writer.flush();
                    if (writer.checkError()) dead.add(writer);
                } catch (Exception e) {
                    dead.add(writer);
                }
            }
            writers.removeAll(dead);
        }
        logger.fine("Broadcast to " + (writers.size()) + " subscribers for auction " + auctionId);
    }
}
