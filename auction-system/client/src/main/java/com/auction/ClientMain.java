package com.auction;

import com.auction.util.AppContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientMain extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("Online Auction System");
        stage.setMinWidth(800);
        stage.setMinHeight(600);

        try {
            AppContext.getInstance().getConnection().connect("localhost", 9090);
        } catch (IOException e) {
            showAlert("Khong the ket noi server!\nHay chay ServerMain truoc.\nPort: 9090\nLoi: " + e.getMessage());
            return;
        }
        loadScene("/fxml/Login.fxml", "Dang nhap - He thong Dau gia");
    }

    public static void loadScene(String fxmlPath, String title) throws IOException {
        URL fxmlUrl = getFxmlUrl(fxmlPath);
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        URL cssUrl = getCssUrl("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        primaryStage.setScene(scene);
        primaryStage.setTitle(title);
        primaryStage.show();
    }

    public static <T> T loadSceneWithController(String fxmlPath, String title) throws IOException {
        URL fxmlUrl = getFxmlUrl(fxmlPath);
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        URL cssUrl = getCssUrl("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        primaryStage.setScene(scene);
        primaryStage.setTitle(title);
        primaryStage.show();
        return loader.getController();
    }

    /**
     * Tries multiple locations to find FXML:
     * 1. classpath (target/classes) - normal
     * 2. src/main/resources - fallback for IntelliJ when resources not copied
     */
    private static URL getFxmlUrl(String fxmlPath) throws IOException {
        // 1. Try classpath first
        URL url = ClientMain.class.getResource(fxmlPath);
        if (url != null) {
            // Verify it's not an empty/corrupt file
            try {
                if (url.openStream().read() > 0) {
                    return url;
                }
            } catch (Exception ignored) {}
        }

        // 2. Fallback: find src/main/resources relative to working directory
        String[] bases = {
            "client/src/main/resources",
            "../client/src/main/resources",
            "src/main/resources"
        };
        for (String base : bases) {
            Path p = Paths.get(base + fxmlPath);
            if (Files.exists(p) && Files.size(p) > 0) {
                return p.toUri().toURL();
            }
        }

        throw new IOException(
            "FXML not found: " + fxmlPath + "\n" +
            "Thu thu cong: copy thu muc 'resources/fxml' vao 'target/classes/fxml'"
        );
    }

    private static URL getCssUrl(String cssPath) {
        URL url = ClientMain.class.getResource(cssPath);
        if (url != null) return url;
        String[] bases = {
            "client/src/main/resources",
            "../client/src/main/resources",
            "src/main/resources"
        };
        for (String base : bases) {
            Path p = Paths.get(base + cssPath);
            if (Files.exists(p)) {
                try { return p.toUri().toURL(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Loi khoi dong");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        AppContext.getInstance().getConnection().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
