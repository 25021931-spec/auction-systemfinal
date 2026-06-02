package com.auction.controller;

import com.auction.ClientMain;
import com.auction.dto.AuctionDto;
import com.auction.dto.BidDto;
import com.auction.dto.SocketMessage;
import com.auction.enums.MessageType;
import com.auction.util.AppContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * MVC Controller for the live bidding screen.
 * Handles manual bidding, auto-bid setup, and real-time chart updates via Observer push.
 */
public class BiddingController {

    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label statusLabel;
    @FXML private Label countdownLabel;

    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Button placeBidBtn;
    @FXML private Button autoBidBtn;

    @FXML private TableView<BidDto> bidHistoryTable;
    @FXML private TableColumn<BidDto, String> bidderCol;
    @FXML private TableColumn<BidDto, Double> amountCol;
    @FXML private TableColumn<BidDto, String> timeCol;
    @FXML private TableColumn<BidDto, String> autoCol;

    @FXML private LineChart<String, Number> priceChart;

    private AuctionDto currentAuction;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ObservableList<BidDto> bids = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private Consumer<SocketMessage> pushListener;
    private final ScheduledExecutorService countdownScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> countdownTask;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        setupTable();
        priceSeries.setName("Bid Price");
        priceChart.getData().add(priceSeries);

        // Register push listener for realtime updates
        pushListener = this::handleServerPush;
        AppContext.getInstance().getConnection().addPushListener(pushListener);
    }

    public void setAuction(AuctionDto auction) {
        // First fetch full detail
        new Thread(() -> {
            try {
                SocketMessage req = new SocketMessage(MessageType.GET_AUCTION_DETAIL, auction.getId());
                SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.SUCCESS) {
                        AuctionDto full = objectMapper.convertValue(resp.getPayload(), AuctionDto.class);
                        updateUI(full);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error loading auction: " + e.getMessage()));
            }
        }).start();
    }

    private void handleServerPush(SocketMessage msg) {
        if (currentAuction == null) return;
        Platform.runLater(() -> {
            try {
                AuctionDto updated = objectMapper.convertValue(msg.getPayload(), AuctionDto.class);
                if (!updated.getId().equals(currentAuction.getId())) return;

                updateUI(updated);

                if (msg.getType() == MessageType.AUCTION_EXTENDED) {
                    showAlert("⏱ Auction Extended!", "A bid was placed in the last 30 seconds.\nEnd time extended.");
                } else if (msg.getType() == MessageType.AUCTION_ENDED) {
                    placeBidBtn.setDisable(true);
                    autoBidBtn.setDisable(true);
                    String winner = updated.getCurrentLeader();
                    showAlert("🏆 Auction Ended!", "Winner: " + (winner != null ? winner : "No bids"));
                }
            } catch (Exception e) {
                statusLabel.setText("Update error: " + e.getMessage());
            }
        });
    }

    private void updateUI(AuctionDto auction) {
        this.currentAuction = auction;
        itemNameLabel.setText(auction.getItemName());
        currentPriceLabel.setText(String.format("%.2f VND", auction.getCurrentPrice()));
        leaderLabel.setText(auction.getCurrentLeader() != null ? auction.getCurrentLeader() : "(no bids yet)");
        endTimeLabel.setText(auction.getEndTime() != null
                ? auction.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        statusLabel.setText(auction.getStatus() != null ? auction.getStatus().name() : "");

        if (auction.getBidHistory() != null) {
            bids.setAll(auction.getBidHistory());
            // Add to chart
            for (BidDto bid : auction.getBidHistory()) {
                String time = bid.getBidTime() != null ? bid.getBidTime().format(FMT) : "";
                boolean exists = priceSeries.getData().stream()
                        .anyMatch(d -> d.getXValue().equals(time) && d.getYValue().doubleValue() == bid.getAmount());
                if (!exists) {
                    priceSeries.getData().add(new XYChart.Data<>(time, bid.getAmount()));
                }
            }
        }

        // Also update chart with current price
        String now = LocalDateTime.now().format(FMT);
        priceSeries.getData().add(new XYChart.Data<>(now, auction.getCurrentPrice()));

        // Disable bidding if not running
        boolean canBid = "RUNNING".equals(auction.getStatus() != null ? auction.getStatus().name() : "");
        placeBidBtn.setDisable(!canBid);
        autoBidBtn.setDisable(!canBid);

        startCountdown(auction);
    }

    private void startCountdown(AuctionDto auction) {
        if (countdownTask != null) countdownTask.cancel(false);
        if (auction.getEndTime() == null) return;
        countdownTask = countdownScheduler.scheduleAtFixedRate(() -> {
            long secs = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
            String display = secs > 0
                    ? String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60)
                    : "ENDED";
            Platform.runLater(() -> countdownLabel.setText(display));
        }, 0, 1, TimeUnit.SECONDS);
    }

    @FXML
    private void handlePlaceBid() {
        if (currentAuction == null) return;
        String amtStr = bidAmountField.getText().trim();
        try {
            double amount = Double.parseDouble(amtStr);
            new Thread(() -> {
                try {
                    BidDto dto = new BidDto();
                    dto.setAuctionId(currentAuction.getId());
                    dto.setBidderId(AppContext.getInstance().getCurrentUser().getId());
                    dto.setBidderName(AppContext.getInstance().getCurrentUser().getUsername());
                    dto.setAmount(amount);

                    SocketMessage req = new SocketMessage(MessageType.PLACE_BID, dto);
                    SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                    Platform.runLater(() -> {
                        if (resp.getType() == MessageType.SUCCESS) {
                            AuctionDto updated = objectMapper.convertValue(resp.getPayload(), AuctionDto.class);
                            updateUI(updated);
                            bidAmountField.clear();
                        } else {
                            showAlert("Bid Failed", resp.getError());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Error", e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid bid amount.");
        }
    }

    @FXML
    private void handleAutoBid() {
        if (currentAuction == null) return;
        try {
            double maxBid = Double.parseDouble(maxBidField.getText().trim());
            double increment = Double.parseDouble(incrementField.getText().trim());

            new Thread(() -> {
                try {
                    BidDto dto = new BidDto();
                    dto.setAuctionId(currentAuction.getId());
                    dto.setBidderId(AppContext.getInstance().getCurrentUser().getId());
                    dto.setBidderName(AppContext.getInstance().getCurrentUser().getUsername());
                    dto.setMaxBid(maxBid);
                    dto.setIncrement(increment);

                    SocketMessage req = new SocketMessage(MessageType.AUTO_BID, dto);
                    SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                    Platform.runLater(() -> {
                        if (resp.getType() == MessageType.SUCCESS) {
                            showAlert("Auto-Bid Set", "Auto-bidding activated up to " + maxBid);
                        } else {
                            showAlert("Auto-Bid Failed", resp.getError());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Error", e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid numbers for max bid and increment.");
        }
    }

    @FXML
    private void handleBack() throws Exception {
        AppContext.getInstance().getConnection().removePushListener(pushListener);
        if (countdownTask != null) countdownTask.cancel(false);
        ClientMain.loadScene("/fxml/AuctionList.fxml", "Auctions");
    }

    private void setupTable() {
        bidderCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("bidderName"));
        amountCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
        timeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getBidTime() != null
                                ? data.getValue().getBidTime().format(FMT) : ""
                ));
        autoCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().isAutoBid() ? "AUTO" : "MANUAL"
                ));
        bidHistoryTable.setItems(bids);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
