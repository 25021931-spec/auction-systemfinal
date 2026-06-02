package com.auction.service;

import com.auction.dto.UserDto;
import com.auction.enums.UserRole;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private static UserService userService;
    private static final String TEST_USER = "testuser_" + System.currentTimeMillis();
    private static final String TEST_EMAIL = TEST_USER + "@test.com";
    private static String sessionToken;

    @BeforeAll
    static void setUp() {
        userService = new UserService();
    }

    @Test
    @Order(1)
    void testRegisterBidder() throws AuctionException {
        UserDto dto = new UserDto();
        dto.setUsername(TEST_USER);
        dto.setPassword("password123");
        dto.setEmail(TEST_EMAIL);
        dto.setRole(UserRole.BIDDER);

        UserDto result = userService.register(dto);
        assertNotNull(result.getId(), "ID should be assigned");
        assertEquals(TEST_USER, result.getUsername());
        assertEquals(UserRole.BIDDER, result.getRole());
        assertTrue(result.isActive());
    }

    @Test
    @Order(2)
    void testRegisterDuplicateUsername() {
        UserDto dto = new UserDto();
        dto.setUsername(TEST_USER); // same username
        dto.setPassword("different123");
        dto.setEmail("other_" + TEST_EMAIL);
        dto.setRole(UserRole.BIDDER);

        assertThrows(AuctionException.class, () -> userService.register(dto),
                "Duplicate username should throw AuctionException");
    }

    @Test
    @Order(3)
    void testRegisterInvalidEmail() {
        UserDto dto = new UserDto();
        dto.setUsername("newuser_" + System.currentTimeMillis());
        dto.setPassword("password123");
        dto.setEmail("notanemail"); // no @
        dto.setRole(UserRole.BIDDER);

        assertThrows(AuctionException.class, () -> userService.register(dto),
                "Invalid email should throw AuctionException");
    }

    @Test
    @Order(4)
    void testRegisterWeakPassword() {
        UserDto dto = new UserDto();
        dto.setUsername("newuser2_" + System.currentTimeMillis());
        dto.setPassword("abc"); // too short
        dto.setEmail("valid@email.com");
        dto.setRole(UserRole.BIDDER);

        assertThrows(AuctionException.class, () -> userService.register(dto),
                "Short password should throw AuctionException");
    }

    @Test
    @Order(5)
    void testLoginSuccess() throws AuctionException {
        UserDto result = userService.login(TEST_USER, "password123");
        assertNotNull(result.getToken(), "Token should be issued on login");
        assertEquals(TEST_USER, result.getUsername());
        sessionToken = result.getToken();
    }

    @Test
    @Order(6)
    void testLoginWrongPassword() {
        assertThrows(AuctionException.class,
                () -> userService.login(TEST_USER, "wrongpassword"),
                "Wrong password should fail");
    }

    @Test
    @Order(7)
    void testLoginNonExistentUser() {
        assertThrows(AuctionException.class,
                () -> userService.login("nobody_xyz_123", "password"),
                "Non-existent user should fail login");
    }

    @Test
    @Order(8)
    void testValidateToken() {
        assertNotNull(sessionToken, "Token from login should be available");
        Optional<UserDto> user = userService.validateToken(sessionToken);
        assertTrue(user.isPresent(), "Valid token should return user");
        assertEquals(TEST_USER, user.get().getUsername());
    }

    @Test
    @Order(9)
    void testValidateInvalidToken() {
        Optional<UserDto> user = userService.validateToken("fake-token-12345");
        assertFalse(user.isPresent(), "Invalid token should return empty");
    }

    @Test
    @Order(10)
    void testLogoutInvalidatesToken() throws AuctionException {
        UserDto loginResult = userService.login(TEST_USER, "password123");
        String token = loginResult.getToken();

        assertTrue(userService.validateToken(token).isPresent(), "Token should be valid before logout");

        userService.logout(token);

        assertFalse(userService.validateToken(token).isPresent(), "Token should be invalid after logout");
    }

    @Test
    @Order(11)
    void testGetAllUsers() throws AuctionException {
        List<UserDto> users = userService.getAllUsers();
        assertNotNull(users);
        assertFalse(users.isEmpty(), "Should have at least admin and test user");
        assertTrue(users.stream().anyMatch(u -> "admin".equals(u.getUsername())),
                "Admin user should exist");
    }

    @Test
    @Order(12)
    void testRegisterSeller() throws AuctionException {
        String sellerName = "seller_" + System.currentTimeMillis();
        UserDto dto = new UserDto();
        dto.setUsername(sellerName);
        dto.setPassword("sellerpass123");
        dto.setEmail(sellerName + "@test.com");
        dto.setRole(UserRole.SELLER);

        UserDto result = userService.register(dto);
        assertEquals(UserRole.SELLER, result.getRole(), "Role should be SELLER");
    }
}
