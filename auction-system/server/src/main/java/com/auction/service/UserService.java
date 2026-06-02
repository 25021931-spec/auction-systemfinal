package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.dto.UserDto;
import com.auction.enums.UserRole;
import com.auction.model.user.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class UserService {

    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    private final UserDao userDao;
    // Simple in-memory session tokens: token -> userId
    private final Map<String, Long> sessions = Collections.synchronizedMap(new HashMap<>());

    public UserService() {
        this.userDao = new UserDao();
    }

    public UserDto register(UserDto dto) throws AuctionException {
        try {
            validateRegistration(dto);
            String hash = UserDao.hashPassword(dto.getPassword());
            User user = switch (dto.getRole()) {
                case SELLER -> new Seller(dto.getUsername(), hash, dto.getEmail());
                case ADMIN -> new Admin(dto.getUsername(), hash, dto.getEmail());
                default -> new Bidder(dto.getUsername(), hash, dto.getEmail());
            };
            userDao.save(user);
            logger.info("Registered user: " + user.getUsername());
            return toDto(user);
        } catch (SQLException e) {
            throw new AuctionException("Registration failed: " + e.getMessage(), e);
        }
    }

    public UserDto login(String username, String password) throws AuctionException {
        try {
            User user = userDao.authenticate(username, password)
                    .orElseThrow(() -> new AuctionException("Invalid username or password"));
            String token = UUID.randomUUID().toString();
            sessions.put(token, user.getId());
            UserDto dto = toDto(user);
            dto.setToken(token);
            return dto;
        } catch (SQLException e) {
            throw new AuctionException("Login failed: " + e.getMessage(), e);
        }
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public Optional<UserDto> validateToken(String token) {
        Long userId = sessions.get(token);
        if (userId == null) return Optional.empty();
        try {
            return userDao.findById(userId).map(this::toDto);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public List<UserDto> getAllUsers() throws AuctionException {
        try {
            return userDao.findAll().stream().map(this::toDto).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new AuctionException("Failed to fetch users", e);
        }
    }

    public void banUser(Long userId) throws AuctionException {
        try {
            User user = userDao.findById(userId)
                    .orElseThrow(() -> new AuctionException("User not found: " + userId));
            user.setActive(false);
            userDao.save(user);
        } catch (SQLException e) {
            throw new AuctionException("Failed to ban user", e);
        }
    }

    private void validateRegistration(UserDto dto) throws AuctionException, SQLException {
        if (dto.getUsername() == null || dto.getUsername().length() < 3) {
            throw new AuctionException("Username must be at least 3 characters");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new AuctionException("Password must be at least 6 characters");
        }
        if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
            throw new AuctionException("Invalid email address");
        }
        if (dto.getRole() == null) {
            dto.setRole(UserRole.BIDDER);
        }
        if (userDao.usernameExists(dto.getUsername())) {
            throw new AuctionException("Username already taken: " + dto.getUsername());
        }
        if (userDao.emailExists(dto.getEmail())) {
            throw new AuctionException("Email already registered: " + dto.getEmail());
        }
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.isActive());
    }
}
