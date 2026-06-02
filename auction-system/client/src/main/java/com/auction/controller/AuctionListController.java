package com.auction.controller;

import com.auction.ClientMain;
import com.auction.dto.AuctionDto;
import com.auction.dto.SocketMessage;
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

import java.util.List;

public class AuctionListController {

    @FXML private TableView<AuctionDto> auctionTable;
    @FXML private TableColumn<AuctionDto, Long> idCol;
    @FXML private TableColumn<AuctionDto, String> nameCol;
    @FXML private TableColumn<AuctionDto, String> categoryCol;
    @FXML private TableColumn<AuctionDto, Double> priceCol;
    @FXML private TableColumn<AuctionDto, String> leaderCol;
    @FXML private TableColumn<AuctionDto, String> endTimeCol;
    @FXML private TableColumn<AuctionDto, String> statusCol;
    @FXML private Label userLabel;
    @FXML private Label statusBar;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final ObservableList<AuctionDto> auctions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        userLabel.setText("Logged in as: " + AppContext.getInstance().getCurrentUser().getUsername());
        loadAuctions();
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        leaderCol.setCellValueFactory(new PropertyValueFactory<>("currentLeader"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        endTimeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getEndTime() != null
                                ? data.getValue().getEndTime().toString().replace("T", " ")
                                : ""
                )
        );

        auctionTable.setItems(auctions);
        auctionTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                AuctionDto selected = auctionTable.getSelectionModel().getSelectedItem();
                if (selected != null) openBidding(selected);
            }
        });
    }

    @FXML
    private void loadAuctions() {
        statusBar.setText("Loading auctions...");
        new Thread(() -> {
            try {
                SocketMessage req = new SocketMessage(MessageType.GET_AUCTIONS, null);
                SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.SUCCESS) {
                        List<AuctionDto> list = objectMapper.convertValue(
                                resp.getPayload(), new TypeReference<>() {});
                        auctions.setAll(list);
                        statusBar.setText("Loaded " + list.size() + " auctions");
                    } else {
                        statusBar.setText("Error: " + resp.getError());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusBar.setText("Connection error: " + e.getMessage()));
            }
        }).start();
    }

    private void openBidding(AuctionDto auction) {
        try {
            BiddingController controller = ClientMain.loadSceneWithController(
                    "/fxml/Bidding.fxml", "Bidding: " + auction.getItemName());
            controller.setAuction(auction);
        } catch (Exception e) {
            statusBar.setText("Error opening auction: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() throws Exception {
        AppContext.getInstance().logout();
        ClientMain.loadScene("/fxml/Login.fxml", "Login");
    }
}
