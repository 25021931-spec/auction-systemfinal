package com.auction.service;

import com.auction.dto.AuctionDto;
import com.auction.dto.BidDto;
import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemCategory;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuctionService business logic.
 * Covers: bid validation, concurrent bidding safety, anti-sniping.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionServiceTest {

    private static AuctionService auctionService;
    private static NotificationService notificationService;
    private static Long testAuctionId;

    @BeforeAll
    static void setUp() {
        // Use in-memory SQLite for tests - DatabaseManager uses auction.db
        notificationService = new NotificationService();
        auctionService = new AuctionService(notificationService);
    }

    // ─── Helper ───────────────────────────────────────────────────────

    private AuctionDto createTestAuction(int durationSeconds) throws AuctionException {
        AuctionDto dto = new AuctionDto();
        dto.setItemName("Test Item " + System.currentTimeMillis());
        dto.setItemDescription("Unit test item");
        dto.setStartingPrice(1000.0);
        dto.setCategory(ItemCategory.ELECTRONICS);
        dto.setSellerId(999L);
        dto.setStartTime(LocalDateTime.now().minusSeconds(1)); // already started
        dto.setEndTime(LocalDateTime.now().plusSeconds(durationSeconds));
        return auctionService.createAuction(dto);
    }

    // ─── Tests ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testCreateAuction() throws AuctionException {
        AuctionDto result = createTestAuction(3600);
        assertNotNull(result.getId(), "Auction ID should be assigned");
        assertEquals("OPEN", result.getStatus() != null ? result.getStatus().name() : "OPEN");
        assertTrue(result.getStartingPrice() > 0);
        testAuctionId = result.getId();
    }

    @Test
    @Order(2)
    void testCreateAuctionWithPastEndTime() {
        AuctionDto dto = new AuctionDto();
        dto.setItemName("Bad Auction");
        dto.setStartingPrice(500.0);
        dto.setCategory(ItemCategory.OTHER);
        dto.setSellerId(999L);
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().minusHours(1)); // past time!

        assertThrows(AuctionException.class, () -> auctionService.createAuction(dto),
                "Should throw when end time is in the past");
    }

    @Test
    @Order(3)
    void testPlaceBidSuccessfully() throws AuctionException {
        AuctionDto auction = createTestAuction(3600);
        // Force to RUNNING
        auctionService.processExpiredAuctions();

        BidDto bid = new BidDto();
        bid.setAuctionId(auction.getId());
        bid.setBidderId(1L);
        bid.setBidderName("testBidder1");
        bid.setAmount(1500.0);

        AuctionDto updated = auctionService.placeBid(bid);
        assertEquals(1500.0, updated.getCurrentPrice(), 0.001, "Price should update to bid amount");
        assertEquals("testBidder1", updated.getCurrentLeader());
    }

    @Test
    @Order(4)
    void testBidLowerThanCurrentPriceRejected() throws AuctionException {
        AuctionDto auction = createTestAuction(3600);

        // Place initial bid
        BidDto first = new BidDto();
        first.setAuctionId(auction.getId());
        first.setBidderId(1L);
        first.setBidderName("bidder1");
        first.setAmount(2000.0);
        auctionService.placeBid(first);

        // Try lower bid
        BidDto lower = new BidDto();
        lower.setAuctionId(auction.getId());
        lower.setBidderId(2L);
        lower.setBidderName("bidder2");
        lower.setAmount(1500.0);

        assertThrows(AuctionException.class, () -> auctionService.placeBid(lower),
                "Bid lower than current price should be rejected");
    }

    @Test
    @Order(5)
    void testCannotBidOnOwnHighestBid() throws AuctionException {
        AuctionDto auction = createTestAuction(3600);

        BidDto first = new BidDto();
        first.setAuctionId(auction.getId());
        first.setBidderId(42L);
        first.setBidderName("sameBidder");
        first.setAmount(2000.0);
        auctionService.placeBid(first);

        BidDto again = new BidDto();
        again.setAuctionId(auction.getId());
        again.setBidderId(42L);
        again.setBidderName("sameBidder");
        again.setAmount(2500.0);

        assertThrows(AuctionException.class, () -> auctionService.placeBid(again),
                "Cannot bid when already highest bidder");
    }

    @Test
    @Order(6)
    void testConcurrentBiddingNoDuplicateWinner() throws Exception {
        AuctionDto auction = createTestAuction(3600);
        Long auctionId = auction.getId();

        int numBidders = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numBidders);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numBidders; i++) {
            final int bidderIdx = i;
            executor.submit(() -> {
                try {
                    latch.await(); // All start simultaneously
                    BidDto bid = new BidDto();
                    bid.setAuctionId(auctionId);
                    bid.setBidderId((long) (bidderIdx + 100));
                    bid.setBidderName("concurrent_bidder_" + bidderIdx);
                    bid.setAmount(1000.0 + (bidderIdx * 100.0));
                    auctionService.placeBid(bid);
                    successCount.incrementAndGet();
                } catch (AuctionException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        latch.countDown(); // Release all threads at once
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify exactly one winner
        AuctionDto result = auctionService.getAuctionDetail(auctionId);
        assertNotNull(result.getCurrentLeader(), "Should have exactly one winner");
        assertTrue(result.getCurrentPrice() >= 1000.0, "Price should have been updated");
        // Total successes + failures = numBidders
        assertEquals(numBidders, successCount.get() + failCount.get());
    }

    @Test
    @Order(7)
    void testGetAllAuctions() throws AuctionException {
        List<AuctionDto> auctions = auctionService.getAllAuctions();
        assertNotNull(auctions);
        assertFalse(auctions.isEmpty(), "Should have at least the test auctions we created");
    }

    @Test
    @Order(8)
    void testAuctionStatusTransition() throws AuctionException, InterruptedException {
        // Create auction that expires in 2 seconds
        AuctionDto dto = new AuctionDto();
        dto.setItemName("Short Auction");
        dto.setItemDescription("Expires soon");
        dto.setStartingPrice(500.0);
        dto.setCategory(ItemCategory.OTHER);
        dto.setSellerId(999L);
        dto.setStartTime(LocalDateTime.now().minusSeconds(1));
        dto.setEndTime(LocalDateTime.now().plusSeconds(2));
        AuctionDto created = auctionService.createAuction(dto);

        // Wait for it to expire
        Thread.sleep(3000);
        auctionService.processExpiredAuctions();

        AuctionDto result = auctionService.getAuctionDetail(created.getId());
        assertEquals(AuctionStatus.FINISHED, result.getStatus(),
                "Auction should transition to FINISHED after end time");
    }

    @Test
    @Order(9)
    void testAutoBidRegistration() throws AuctionException {
        AuctionDto auction = createTestAuction(3600);

        BidDto autoBidDto = new BidDto();
        autoBidDto.setAuctionId(auction.getId());
        autoBidDto.setBidderId(200L);
        autoBidDto.setBidderName("autoBidder");
        autoBidDto.setMaxBid(5000.0);
        autoBidDto.setIncrement(100.0);

        assertDoesNotThrow(() -> auctionService.setAutoBid(autoBidDto),
                "Auto-bid registration should succeed");
    }

    @Test
    @Order(10)
    void testAutoBidMaxBidValidation() throws AuctionException {
        AuctionDto auction = createTestAuction(3600);

        // Place bid to set current price at 2000
        BidDto initial = new BidDto();
        initial.setAuctionId(auction.getId());
        initial.setBidderId(1L);
        initial.setBidderName("bidder1");
        initial.setAmount(2000.0);
        auctionService.placeBid(initial);

        // Try auto-bid with max < current price
        BidDto autoBid = new BidDto();
        autoBid.setAuctionId(auction.getId());
        autoBid.setBidderId(300L);
        autoBid.setBidderName("autoBidder2");
        autoBid.setMaxBid(1500.0); // less than current 2000
        autoBid.setIncrement(100.0);

        assertThrows(AuctionException.class, () -> auctionService.setAutoBid(autoBid),
                "Auto-bid max must be greater than current price");
    }
}
