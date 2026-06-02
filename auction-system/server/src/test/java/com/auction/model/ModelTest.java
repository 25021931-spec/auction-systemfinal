package com.auction.model;

import com.auction.enums.ItemCategory;
import com.auction.enums.UserRole;
import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.item.*;
import com.auction.model.user.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests OOP hierarchy: Inheritance, Polymorphism, Encapsulation.
 */
class ModelTest {

    // ─── Item Hierarchy ───────────────────────────────────────────────

    @Test
    void testElectronicsInheritsItem() {
        Electronics e = new Electronics("MacBook Pro", "Laptop", 25000000, 1L,
                "Apple", "M3", 12);
        assertInstanceOf(Item.class, e, "Electronics should be an Item");
        assertInstanceOf(com.auction.model.entity.Entity.class, e, "Electronics should be an Entity");
        assertEquals(ItemCategory.ELECTRONICS, e.getCategory());
        assertTrue(e.getInfo().contains("MacBook Pro"));
        assertTrue(e.getCategoryDetails().contains("Apple"));
    }

    @Test
    void testArtInheritsItem() {
        Art art = new Art("Mona Lisa", "Famous painting", 1000000, 1L,
                "Da Vinci", 1503, "Oil on poplar");
        assertInstanceOf(Item.class, art);
        assertEquals(ItemCategory.ART, art.getCategory());
        assertTrue(art.getCategoryDetails().contains("Da Vinci"));
    }

    @Test
    void testVehicleInheritsItem() {
        Vehicle v = new Vehicle("Toyota Camry", "Sedan", 500000000, 1L,
                "Toyota", "Camry", 2022, 30000);
        assertInstanceOf(Item.class, v);
        assertEquals(ItemCategory.VEHICLE, v.getCategory());
        assertTrue(v.getInfo().contains("Toyota"));
    }

    @Test
    void testPolymorphismGetInfo() {
        Item[] items = {
            new Electronics("Phone", "Desc", 5000000, 1L, "Samsung", "S24", 12),
            new Art("Painting", "Desc", 2000000, 1L, "Picasso", 1930, "Oil"),
            new Vehicle("Car", "Desc", 300000000, 1L, "Honda", "Civic", 2020, 10000),
            new OtherItem("Misc", "Desc", 100000, 1L)
        };
        for (Item item : items) {
            assertNotNull(item.getInfo(), "getInfo() should not return null for " + item.getClass().getSimpleName());
            assertNotNull(item.getCategoryDetails(), "getCategoryDetails() should not return null");
        }
    }

    @Test
    void testItemFactoryCreatesCorrectSubtype() {
        Item electronics = ItemFactory.createItem(ItemCategory.ELECTRONICS, "TV", "Desc", 10000, 1L);
        Item art = ItemFactory.createItem(ItemCategory.ART, "Statue", "Desc", 50000, 1L);
        Item vehicle = ItemFactory.createItem(ItemCategory.VEHICLE, "Bike", "Desc", 30000, 1L);
        Item other = ItemFactory.createItem(ItemCategory.OTHER, "Misc", "Desc", 1000, 1L);

        assertInstanceOf(Electronics.class, electronics);
        assertInstanceOf(Art.class, art);
        assertInstanceOf(Vehicle.class, vehicle);
        assertInstanceOf(OtherItem.class, other);
    }

    // ─── User Hierarchy ───────────────────────────────────────────────

    @Test
    void testBidderPermissions() {
        Bidder b = new Bidder("alice", "hash", "alice@test.com");
        assertInstanceOf(User.class, b);
        assertTrue(b.canBid(), "Bidder should be able to bid");
        assertFalse(b.canSell(), "Bidder cannot sell");
        assertFalse(b.canManage(), "Bidder cannot manage");
        assertEquals(UserRole.BIDDER, b.getRole());
    }

    @Test
    void testSellerPermissions() {
        Seller s = new Seller("bob", "hash", "bob@test.com");
        assertFalse(s.canBid(), "Seller cannot bid");
        assertTrue(s.canSell(), "Seller can sell");
        assertFalse(s.canManage(), "Seller cannot manage");
        assertEquals(UserRole.SELLER, s.getRole());
    }

    @Test
    void testAdminPermissions() {
        Admin a = new Admin("admin", "hash", "admin@test.com");
        assertFalse(a.canBid(), "Admin cannot bid");
        assertFalse(a.canSell(), "Admin cannot sell");
        assertTrue(a.canManage(), "Admin can manage");
        assertEquals(UserRole.ADMIN, a.getRole());
    }

    @Test
    void testEncapsulationUserPassword() {
        Bidder b = new Bidder("user1", "secret_hash", "u@test.com");
        // Password hash is accessible only via getter (encapsulated)
        assertNotNull(b.getPasswordHash());
        assertEquals("secret_hash", b.getPasswordHash());
        // Can update via setter
        b.setPasswordHash("new_hash");
        assertEquals("new_hash", b.getPasswordHash());
    }

    // ─── Auction Model ────────────────────────────────────────────────

    @Test
    void testAuctionStatusTransition() {
        Item item = new Electronics("Test", "Desc", 1000, 1L, "Brand", "M1", 12);
        LocalDateTime start = LocalDateTime.now().minusSeconds(10);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Auction auction = new Auction(item, 1L, start, end);

        auction.updateStatus();
        assertEquals(com.auction.enums.AuctionStatus.RUNNING, auction.getStatus(),
                "Should be RUNNING after start time passes");
    }

    @Test
    void testAuctionAntiSnipingExtension() {
        Item item = new Electronics("Test", "Desc", 1000, 1L, "Brand", "M1", 12);
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusSeconds(15); // 15s left = within snipe window
        Auction auction = new Auction(item, 1L, start, end);
        auction.updateStatus();

        LocalDateTime beforeExtend = auction.getEndTime();
        boolean extended = auction.checkAndExtend();

        assertTrue(extended, "Should extend when bid in last 30 seconds");
        assertTrue(auction.getEndTime().isAfter(beforeExtend),
                "End time should be pushed forward");
    }

    @Test
    void testBidTransaction() {
        BidTransaction bid = new BidTransaction(1L, 2L, "alice", 5000.0, false);
        assertNotNull(bid.getBidTime(), "Bid time should auto-set");
        assertEquals(1L, bid.getAuctionId());
        assertEquals("alice", bid.getBidderName());
        assertEquals(5000.0, bid.getAmount(), 0.001);
        assertFalse(bid.isAutoBid());
        assertTrue(bid.getInfo().contains("alice"));
    }
}
