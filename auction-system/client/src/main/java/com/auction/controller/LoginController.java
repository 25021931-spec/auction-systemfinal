package com.auction.controller;

import com.auction.ClientMain;
import com.auction.dto.SocketMessage;
import com.auction.dto.UserDto;
import com.auction.enums.MessageType;
import com.auction.enums.UserRole;
import com.auction.util.AppContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private TextField regEmailField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label messageLabel;
    @FXML private TabPane tabPane;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        System.out.println("[LOGIN] initialize() called");
        System.out.println("[LOGIN] loginUsernameField = " + loginUsernameField);
        System.out.println("[LOGIN] loginPasswordField = " + loginPasswordField);
        System.out.println("[LOGIN] roleCombo = " + roleCombo);
        System.out.println("[LOGIN] messageLabel = " + messageLabel);

        if (roleCombo != null) {
            roleCombo.getItems().addAll("BIDDER", "SELLER");
            roleCombo.setValue("BIDDER");
        }
    }

    @FXML
    private void handleLogin() {
        System.out.println("[LOGIN] handleLogin() called");

        // null-safe lấy text
        String username = loginUsernameField != null ? loginUsernameField.getText().trim() : "";
        String password = loginPasswordField != null ? loginPasswordField.getText() : "";

        System.out.println("[LOGIN] username=" + username + ", password length=" + password.length());

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Vui long nhap ten dang nhap va mat khau.", true);
            return;
        }

        showMessage("Dang ket noi...", false);

        new Thread(() -> {
            try {
                System.out.println("[LOGIN] Sending login request...");
                UserDto dto = new UserDto();
                dto.setUsername(username);
                dto.setPassword(password);

                SocketMessage req = new SocketMessage(MessageType.LOGIN, dto);
                SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);

                System.out.println("[LOGIN] Response type: " + resp.getType());
                System.out.println("[LOGIN] Response error: " + resp.getError());

                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.SUCCESS) {
                        try {
                            UserDto user = objectMapper.convertValue(resp.getPayload(), UserDto.class);
                            System.out.println("[LOGIN] Login success! Role: " + user.getRole());
                            AppContext.getInstance().setCurrentUser(user);
                            navigateToDashboard(user);
                        } catch (Exception e) {
                            System.out.println("[LOGIN] Navigation error: " + e.getMessage());
                            e.printStackTrace();
                            showMessage("Loi chuyen trang: " + e.getMessage(), true);
                        }
                    } else {
                        String err = resp.getError() != null ? resp.getError() : "Sai ten hoac mat khau";
                        System.out.println("[LOGIN] Login failed: " + err);
                        showMessage(err, true);
                    }
                });
            } catch (Exception e) {
                System.out.println("[LOGIN] Exception: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showMessage("Loi ket noi: " + e.getMessage(), true));
            }
        }, "login-thread").start();
    }

    @FXML
    private void handleRegister() {
        System.out.println("[REGISTER] handleRegister() called");

        String username = regUsernameField != null ? regUsernameField.getText().trim() : "";
        String password = regPasswordField != null ? regPasswordField.getText() : "";
        String email    = regEmailField    != null ? regEmailField.getText().trim()    : "";
        String role     = roleCombo        != null ? roleCombo.getValue()              : "BIDDER";

        System.out.println("[REGISTER] username=" + username + " email=" + email + " role=" + role);

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            showMessage("Vui long nhap day du thong tin.", true);
            return;
        }

        showMessage("Dang xu ly...", false);

        new Thread(() -> {
            try {
                UserDto dto = new UserDto();
                dto.setUsername(username);
                dto.setPassword(password);
                dto.setEmail(email);
                dto.setRole(UserRole.valueOf(role));

                SocketMessage req = new SocketMessage(MessageType.REGISTER, dto);
                SocketMessage resp = AppContext.getInstance().getConnection().sendAndReceive(req);

                System.out.println("[REGISTER] Response: " + resp.getType() + " / " + resp.getError());

                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.SUCCESS) {
                        showMessage("Dang ky thanh cong! Hay dang nhap.", false);
                        if (tabPane != null) tabPane.getSelectionModel().select(0);
                    } else {
                        showMessage(resp.getError() != null ? resp.getError() : "Dang ky that bai", true);
                    }
                });
            } catch (Exception e) {
                System.out.println("[REGISTER] Exception: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showMessage("Loi: " + e.getMessage(), true));
            }
        }, "register-thread").start();
    }

    private void navigateToDashboard(UserDto user) {
        try {
            switch (user.getRole()) {
                case ADMIN  -> ClientMain.loadScene("/fxml/Admin.fxml",       "Admin Dashboard");
                case SELLER -> ClientMain.loadScene("/fxml/Seller.fxml",      "Seller Dashboard");
                default     -> ClientMain.loadScene("/fxml/AuctionList.fxml", "Danh sach dau gia");
            }
        } catch (Exception e) {
            System.out.println("[LOGIN] Navigate error: " + e.getMessage());
            e.printStackTrace();
            showMessage("Loi mo man hinh: " + e.getMessage(), true);
        }
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel == null) {
            System.out.println("[LOGIN] messageLabel is null! msg=" + msg);
            return;
        }
        messageLabel.setText(msg);
        messageLabel.setStyle(isError
                ? "-fx-text-fill: #e85050; -fx-font-size: 12;"
                : "-fx-text-fill: #4dc47a; -fx-font-size: 12;");
    }
}
