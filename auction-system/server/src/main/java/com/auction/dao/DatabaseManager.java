package com.auction.dao;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;
    private Connection connection;

    private static String getDbPath() {
        String userHome = System.getProperty("user.home");
        // dùng dấu / cho cross-platform
        return userHome.replace("\\", "/") + "/auction_system.db";
    }

    private DatabaseManager() {
        try {
            String path = getDbPath();
            logger.info("Database path: " + path);

            // Xóa file cũ nếu bị lock
            deleteOldFiles(path);

            connection = DriverManager.getConnection("jdbc:sqlite:" + path);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 30000");
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            initializeTables();
            logger.info("Database initialized OK.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    private void deleteOldFiles(String path) {
        for (String s : new String[]{"", "-shm", "-wal", "-journal"}) {
            java.io.File f = new java.io.File(path + s);
            if (f.exists() && f.delete()) {
                logger.info("Deleted: " + f.getName());
            }
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() { return connection; }

    private void initializeTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    role TEXT NOT NULL,
                    active INTEGER DEFAULT 1,
                    created_at TEXT,
                    updated_at TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    starting_price REAL NOT NULL,
                    category TEXT NOT NULL,
                    seller_id INTEGER NOT NULL,
                    extra_data TEXT,
                    created_at TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id INTEGER NOT NULL,
                    seller_id INTEGER NOT NULL,
                    current_price REAL NOT NULL,
                    current_leader_id INTEGER,
                    current_leader_name TEXT,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'OPEN',
                    created_at TEXT,
                    updated_at TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bid_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    auction_id INTEGER NOT NULL,
                    bidder_id INTEGER NOT NULL,
                    bidder_name TEXT NOT NULL,
                    amount REAL NOT NULL,
                    is_auto_bid INTEGER DEFAULT 0,
                    bid_time TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auto_bid_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    bidder_id INTEGER NOT NULL,
                    bidder_name TEXT NOT NULL,
                    auction_id INTEGER NOT NULL,
                    max_bid REAL NOT NULL,
                    increment REAL NOT NULL,
                    registered_at TEXT NOT NULL,
                    UNIQUE(bidder_id, auction_id)
                )
            """);

            // Seed admin - hash được tạo lúc runtime nên luôn đúng
            seedAdmin(stmt);
        }
    }

    private void seedAdmin(Statement stmt) throws SQLException {
        // Kiểm tra admin đã tồn tại chưa
        try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM users WHERE username = 'admin'")) {
            if (rs.next() && rs.getInt(1) > 0) {
                logger.info("Admin already exists.");
                return;
            }
        }

        // Tạo hash đúng cách lúc runtime
        String hash = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
        logger.info("Seeding admin account...");

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (username, password_hash, email, role, active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 1, datetime('now'), datetime('now'))")) {
            ps.setString(1, "admin");
            ps.setString(2, hash);
            ps.setString(3, "admin@auction.com");
            ps.setString(4, "ADMIN");
            ps.executeUpdate();
        }
        logger.info("Admin seeded OK. Login: admin / admin123");
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            logger.warning("Error closing DB: " + e.getMessage());
        }
    }
}
