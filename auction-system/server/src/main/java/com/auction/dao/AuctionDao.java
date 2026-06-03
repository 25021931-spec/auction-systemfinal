package com.auction.dao;

import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemCategory;
import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.item.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class AuctionDao {

    private static final Logger logger = Logger.getLogger(AuctionDao.class.getName());
    private final Connection conn;

    public AuctionDao() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ---- Item operations ----

    public Item saveItem(Item item) throws SQLException {
        String sql = """
            INSERT INTO items (name, description, starting_price, category, seller_id, extra_data, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartingPrice());
            ps.setString(4, item.getCategory().name());
            ps.setLong(5, item.getSellerId());
            ps.setString(6, item.getCategoryDetails());
            ps.setString(7, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (Statement st = conn.createStatement();
                 ResultSet keys = st.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) item.setId(keys.getLong(1));
            }
        }
        return item;
    }

    // ---- Auction operations ----

    public Auction saveAuction(Auction auction) throws SQLException {
        if (auction.getId() == null) {
            return insertAuction(auction);
        }
        return updateAuction(auction);
    }

    private Auction insertAuction(Auction auction) throws SQLException {
        // Save item first
        Item savedItem = saveItem(auction.getItem());
        auction.getItem().setId(savedItem.getId());

        String sql = """
            INSERT INTO auctions (item_id, seller_id, current_price, current_leader_id,
                current_leader_name, start_time, end_time, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, savedItem.getId());
            ps.setLong(2, auction.getSellerId());
            ps.setDouble(3, auction.getCurrentPrice());
            if (auction.getCurrentLeaderId() != null) ps.setLong(4, auction.getCurrentLeaderId());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, auction.getCurrentLeaderName());
            ps.setString(6, auction.getStartTime().toString());
            ps.setString(7, auction.getEndTime().toString());
            ps.setString(8, auction.getStatus().name());
            ps.setString(9, LocalDateTime.now().toString());
            ps.setString(10, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (Statement st = conn.createStatement();
                 ResultSet keys = st.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) auction.setId(keys.getLong(1));
            }
        }
        return auction;
    }

    public Auction updateAuction(Auction auction) throws SQLException {
        String sql = """
            UPDATE auctions SET current_price=?, current_leader_id=?,
                current_leader_name=?, end_time=?, status=?, updated_at=?
            WHERE id=?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, auction.getCurrentPrice());
            if (auction.getCurrentLeaderId() != null) ps.setLong(2, auction.getCurrentLeaderId());
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, auction.getCurrentLeaderName());
            ps.setString(4, auction.getEndTime().toString());
            ps.setString(5, auction.getStatus().name());
            ps.setString(6, LocalDateTime.now().toString());
            ps.setLong(7, auction.getId());
            ps.executeUpdate();
        }
        return auction;
    }

    public Optional<Auction> findById(Long id) throws SQLException {
        String sql = """
            SELECT a.*, i.name as item_name, i.description as item_desc,
                   i.starting_price, i.category, i.seller_id as item_seller_id
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Auction auction = mapAuction(rs);
                auction.setBidHistory(findBidsByAuction(id));
                return Optional.of(auction);
            }
        }
        return Optional.empty();
    }

    public List<Auction> findAll() throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = """
            SELECT a.*, i.name as item_name, i.description as item_desc,
                   i.starting_price, i.category, i.seller_id as item_seller_id
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            ORDER BY a.created_at DESC
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                auctions.add(mapAuction(rs));
            }
        }
        return auctions;
    }

    public List<Auction> findByStatus(AuctionStatus status) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = """
            SELECT a.*, i.name as item_name, i.description as item_desc,
                   i.starting_price, i.category, i.seller_id as item_seller_id
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.status = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                auctions.add(mapAuction(rs));
            }
        }
        return auctions;
    }

    public List<BidTransaction> findBidsByAuction(Long auctionId) throws SQLException {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BidTransaction bid = new BidTransaction();
                bid.setId(rs.getLong("id"));
                bid.setAuctionId(rs.getLong("auction_id"));
                bid.setBidderId(rs.getLong("bidder_id"));
                bid.setBidderName(rs.getString("bidder_name"));
                bid.setAmount(rs.getDouble("amount"));
                bid.setAutoBid(rs.getInt("is_auto_bid") == 1);
                bid.setBidTime(LocalDateTime.parse(rs.getString("bid_time")));
                bids.add(bid);
            }
        }
        return bids;
    }

    public BidTransaction saveBid(BidTransaction bid) throws SQLException {
        String sql = """
            INSERT INTO bid_transactions (auction_id, bidder_id, bidder_name, amount, is_auto_bid, bid_time)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bid.getAuctionId());
            ps.setLong(2, bid.getBidderId());
            ps.setString(3, bid.getBidderName());
            ps.setDouble(4, bid.getAmount());
            ps.setInt(5, bid.isAutoBid() ? 1 : 0);
            ps.setString(6, bid.getBidTime().toString());
            ps.executeUpdate();
            try (Statement st = conn.createStatement();
                 ResultSet keys = st.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) bid.setId(keys.getLong(1));
            }
        }
        return bid;
    }

    public void saveAutoBidConfig(com.auction.model.entity.AutoBidConfig config) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO auto_bid_configs
                (bidder_id, bidder_name, auction_id, max_bid, increment, registered_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, config.getBidderId());
            ps.setString(2, config.getBidderName());
            ps.setLong(3, config.getAuctionId());
            ps.setDouble(4, config.getMaxBid());
            ps.setDouble(5, config.getIncrement());
            ps.setString(6, config.getRegisteredAt().toString());
            ps.executeUpdate();
        }
    }

    public List<com.auction.model.entity.AutoBidConfig> findAutoBidConfigs(Long auctionId) throws SQLException {
        List<com.auction.model.entity.AutoBidConfig> configs = new ArrayList<>();
        String sql = "SELECT * FROM auto_bid_configs WHERE auction_id = ? ORDER BY registered_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                com.auction.model.entity.AutoBidConfig c = new com.auction.model.entity.AutoBidConfig();
                c.setBidderId(rs.getLong("bidder_id"));
                c.setBidderName(rs.getString("bidder_name"));
                c.setAuctionId(rs.getLong("auction_id"));
                c.setMaxBid(rs.getDouble("max_bid"));
                c.setIncrement(rs.getDouble("increment"));
                c.setRegisteredAt(LocalDateTime.parse(rs.getString("registered_at")));
                configs.add(c);
            }
        }
        return configs;
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setSellerId(rs.getLong("seller_id"));
        auction.setCurrentPrice(rs.getDouble("current_price"));

        long leaderId = rs.getLong("current_leader_id");
        if (!rs.wasNull()) auction.setCurrentLeaderId(leaderId);
        auction.setCurrentLeaderName(rs.getString("current_leader_name"));
        auction.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
        auction.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));

        // Map item
        ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
        Item item = ItemFactory.createItem(
                category,
                rs.getString("item_name"),
                rs.getString("item_desc"),
                rs.getDouble("starting_price"),
                rs.getLong("item_seller_id")
        );
        auction.setItem(item);
        return auction;
    }
}
