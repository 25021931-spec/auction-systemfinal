package com.auction.controller;

import com.auction.ClientMain;
import com.auction.dto.AuctionDto;
import com.auction.dto.SocketMessage;
import com.auction.enums.ItemCategory;
import com.auction.enums.MessageType;
import com.auction.util.AppContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.util.List;

public class SellerController {

    @FXML private TextField itemNameField;
    @FXML private TextArea itemDescField;
    @FXML private TextField startPriceField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    @FXML private Label messageLabel;

    @FXML private TableView<AuctionDto> myAuctionsTable;
    @FXML private TableColumn<AuctionDto, Long> idCol;
    @FXML private TableColumn<AuctionDto, String> nameCol;
    @FXML private TableColumn<AuctionDto, Double> priceCol;
    @FXML private TableColumn<AuctionDto, String> statusCol;
    @FXML private Label userLabel;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ObservableList<AuctionDto> myAuctions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        categoryCombo.getItems().addAll("ELECTRONICS", "ART", "VEHICLE", "OTHER");
        categoryCombo.setValue("ELECTRONICS");
        userLabel.setText("Seller: " + AppContext.getInstance().getCurrentUser().getUsername());

        setupTable();
        loadMyAuctions();
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        myAuctionsTable.setItems(myAuctions);
    }

    @FXML
    private void handleCreateAuction() {
        try {
            String name = itemNameField.getText().trim();
            String desc = itemDescField.getText().trim();
            double price = Double.parseDouble(startPriceField.getText().trim());
            ItemCategory cat = ItemCategory.valueOf(categoryCombo.getValue());

            if (endDatePicker.getValue() == null) {
                showMessage("Please select an end date.", true);
                return;
            }
            String[] timeParts = endTimeField.getText().trim().split(":");
            LocalDateTime endTime = endDatePicker.getValue().atTime(
                    Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1])
            );

            AuctionDto dto = new AuctionDto();
            dto.setItemName(name);
            dto.setItemDescription(desc);
            dto.setStartingPrice(price);
            dto.setCategory(cat);
            dto.setStartTime(LocalDateTime.now());
            dto.setEndTime(endTime);
            dto.setSellerId(AppContext.getInstance().getCurrentUser().getId());

            new Thread(() -> {
                try {
                    SocketMessage req = new SocketMessage(MessageType.CREATE_AUCTION, dto);
                    SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                    Platform.runLater(() -> {
                        if (resp.getType() == MessageType.SUCCESS) {
                            showMessage("Auction created successfully!", false);
                            clearForm();
                            loadMyAuctions();
                        } else {
                            showMessage("Error: " + resp.getError(), true);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showMessage("Connection error: " + e.getMessage(), true));
                }
            }).start();
        } catch (Exception e) {
            showMessage("Invalid input: " + e.getMessage(), true);
        }
    }

    private void loadMyAuctions() {
        new Thread(() -> {
            try {
                SocketMessage req = new SocketMessage(MessageType.GET_AUCTIONS, null);
                SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.SUCCESS) {
                        List<AuctionDto> all = objectMapper.convertValue(resp.getPayload(), new TypeReference<>() {});
                        Long myId = AppContext.getInstance().getCurrentUser().getId();
                        all.removeIf(a -> !myId.equals(a.getSellerId()));
                        myAuctions.setAll(all);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMessage("Load error: " + e.getMessage(), true));
            }
        }).start();
    }

    private void clearForm() {
        itemNameField.clear();
        itemDescField.clear();
        startPriceField.clear();
        endDatePicker.setValue(null);
        endTimeField.clear();
    }

    @FXML
    private void handleLogout() throws Exception {
        AppContext.getInstance().logout();
        ClientMain.loadScene("/fxml/Login.fxml", "Login");
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill:red;" : "-fx-text-fill:green;");
    }
}
