package com.auction.network;

import com.auction.dto.*;
import com.auction.enums.MessageType;
import com.auction.service.AuctionException;
import com.auction.service.AuctionService;
import com.auction.service.NotificationService;
import com.auction.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles a single client socket connection on its own thread.
 * Parses incoming JSON messages and dispatches to appropriate service.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final AuctionService auctionService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private PrintWriter out;
    private BufferedReader in;
    private UserDto currentUser; // logged-in user for this session

    public ClientHandler(Socket socket, AuctionService auctionService,
                         UserService userService, NotificationService notificationService) {
        this.socket = socket;
        this.auctionService = auctionService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            logger.fine("Client disconnected: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleMessage(String json) {
        try {
            SocketMessage msg = objectMapper.readValue(json, SocketMessage.class);
            MessageType type = msg.getType();

            switch (type) {
                case PING -> send(new SocketMessage(MessageType.PONG, null));
                case REGISTER -> handleRegister(msg);
                case LOGIN -> handleLogin(msg);
                case LOGOUT -> handleLogout();
                case GET_AUCTIONS -> handleGetAuctions();
                case GET_AUCTION_DETAIL -> handleGetAuctionDetail(msg);
                case CREATE_AUCTION -> handleCreateAuction(msg);
                case PLACE_BID -> handlePlaceBid(msg);
                case AUTO_BID -> handleAutoBid(msg);
                case GET_ALL_USERS -> handleGetAllUsers();
                case BAN_USER -> handleBanUser(msg);
                default -> sendError("Unknown message type: " + type);
            }
        } catch (Exception e) {
            logger.warning("Error handling message: " + e.getMessage());
            sendError("Server error: " + e.getMessage());
        }
    }

    private void handleRegister(SocketMessage msg) throws Exception {
        UserDto dto = parsePayload(msg, UserDto.class);
        UserDto result = userService.register(dto);
        send(SocketMessage.success(result));
    }

    private void handleLogin(SocketMessage msg) throws Exception {
        UserDto dto = parsePayload(msg, UserDto.class);
        UserDto result = userService.login(dto.getUsername(), dto.getPassword());
        currentUser = result;
        send(SocketMessage.success(result));
    }

    private void handleLogout() {
        if (currentUser != null && currentUser.getToken() != null) {
            userService.logout(currentUser.getToken());
            notificationService.unsubscribeAll(out);
            currentUser = null;
        }
        send(SocketMessage.success("Logged out"));
    }

    private void handleGetAuctions() throws AuctionException {
        List<AuctionDto> auctions = auctionService.getAllAuctions();
        send(SocketMessage.success(auctions));
    }

    private void handleGetAuctionDetail(SocketMessage msg) throws Exception {
        Long auctionId = parsePayload(msg, Long.class);
        AuctionDto detail = auctionService.getAuctionDetail(auctionId);
        // Subscribe client to real-time updates for this auction
        notificationService.subscribe(auctionId, out);
        send(SocketMessage.success(detail));
    }

    private void handleCreateAuction(SocketMessage msg) throws Exception {
        requireRole("SELLER");
        AuctionDto dto = parsePayload(msg, AuctionDto.class);
        dto.setSellerId(currentUser.getId());
        dto.setSellerName(currentUser.getUsername());
        AuctionDto result = auctionService.createAuction(dto);
        send(SocketMessage.success(result));
    }

    private void handlePlaceBid(SocketMessage msg) throws Exception {
        requireRole("BIDDER");
        BidDto dto = parsePayload(msg, BidDto.class);
        dto.setBidderId(currentUser.getId());
        dto.setBidderName(currentUser.getUsername());
        AuctionDto result = auctionService.placeBid(dto);
        send(SocketMessage.success(result));
    }

    private void handleAutoBid(SocketMessage msg) throws Exception {
        requireRole("BIDDER");
        BidDto dto = parsePayload(msg, BidDto.class);
        dto.setBidderId(currentUser.getId());
        dto.setBidderName(currentUser.getUsername());
        auctionService.setAutoBid(dto);
        send(SocketMessage.success("Auto-bid registered"));
    }

    private void handleGetAllUsers() throws AuctionException {
        requireRole("ADMIN");
        send(SocketMessage.success(userService.getAllUsers()));
    }

    private void handleBanUser(SocketMessage msg) throws Exception {
        requireRole("ADMIN");
        Long userId = parsePayload(msg, Long.class);
        userService.banUser(userId);
        send(SocketMessage.success("User banned"));
    }

    private void requireRole(String role) {
        if (currentUser == null) throw new SecurityException("Not authenticated");
        if (!currentUser.getRole().name().equals(role)) {
            throw new SecurityException("Access denied. Required role: " + role);
        }
    }

    private <T> T parsePayload(SocketMessage msg, Class<T> clazz) throws Exception {
        return objectMapper.convertValue(msg.getPayload(), clazz);
    }

    private synchronized void send(SocketMessage msg) {
        try {
            out.println(objectMapper.writeValueAsString(msg));
            out.flush();
        } catch (Exception e) {
            logger.warning("Failed to send message: " + e.getMessage());
        }
    }

    private void sendError(String message) {
        send(SocketMessage.error(message));
    }

    private void cleanup() {
        notificationService.unsubscribeAll(out);
        try { socket.close(); } catch (IOException ignored) {}
    }
}
