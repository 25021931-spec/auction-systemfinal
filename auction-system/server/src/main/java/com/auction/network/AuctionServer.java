package com.auction.network;

import com.auction.service.AuctionService;
import com.auction.service.NotificationService;
import com.auction.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Main socket server. Accepts client connections and spawns ClientHandler threads.
 * Uses a thread pool for scalable concurrent client handling.
 */
public class AuctionServer {

    private static final Logger logger = Logger.getLogger(AuctionServer.class.getName());
    public static final int DEFAULT_PORT = 9090;

    private final int port;
    private final NotificationService notificationService;
    private final AuctionService auctionService;
    private final UserService userService;
    private final ExecutorService threadPool;
    private final ScheduledExecutorService scheduler;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public AuctionServer(int port) {
        this.port = port;
        this.notificationService = new NotificationService();
        this.auctionService = new AuctionService(notificationService);
        this.userService = new UserService();
        this.threadPool = Executors.newCachedThreadPool();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        // Schedule auction expiration check every 5 seconds
        scheduler.scheduleAtFixedRate(
                auctionService::processExpiredAuctions, 5, 5, TimeUnit.SECONDS
        );

        logger.info("Auction server started on port " + port);
        logger.info("Default admin credentials: admin / admin123");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(
                        clientSocket, auctionService, userService, notificationService
                ));
            } catch (IOException e) {
                if (running) logger.warning("Error accepting connection: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        scheduler.shutdown();
        threadPool.shutdown();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        logger.info("Server stopped.");
    }
}
