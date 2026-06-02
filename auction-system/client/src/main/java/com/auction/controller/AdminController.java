package com.auction.controller;

import com.auction.ClientMain;
import com.auction.dto.SocketMessage;
import com.auction.dto.UserDto;
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

public class AdminController {

    @FXML private TableView<UserDto> usersTable;
    @FXML private TableColumn<UserDto, Long> idCol;
    @FXML private TableColumn<UserDto, String> usernameCol;
    @FXML private TableColumn<UserDto, String> emailCol;
    @FXML private TableColumn<UserDto, String> roleCol;
    @FXML private TableColumn<UserDto, Boolean> activeCol;
    @FXML private Label statusLabel;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ObservableList<UserDto> users = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        usersTable.setItems(users);
        loadUsers();
    }

    @FXML
    private void loadUsers() {
        new Thread(() -> {
            try {
                SocketMessage req = new SocketMessage(MessageType.GET_ALL_USERS, null);
                SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.SUCCESS) {
                        List<UserDto> list = objectMapper.convertValue(resp.getPayload(), new TypeReference<>() {});
                        users.setAll(list);
                        statusLabel.setText("Loaded " + list.size() + " users");
                    } else {
                        statusLabel.setText("Error: " + resp.getError());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleBanUser() {
        UserDto selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Select a user first."); return; }
        if (selected.getId().equals(AppContext.getInstance().getCurrentUser().getId())) {
            statusLabel.setText("Cannot ban yourself.");
            return;
        }
        new Thread(() -> {
            try {
                SocketMessage req = new SocketMessage(MessageType.BAN_USER, selected.getId());
                SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);
                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.SUCCESS) {
                        statusLabel.setText("User banned: " + selected.getUsername());
                        loadUsers();
                    } else {
                        statusLabel.setText("Error: " + resp.getError());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleLogout() throws Exception {
        AppContext.getInstance().logout();
        ClientMain.loadScene("/fxml/Login.fxml", "Login");
    }
}
