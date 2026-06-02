package com.auction.util;

import com.auction.dto.UserDto;
import com.auction.network.ServerConnection;

/**
 * Singleton holding current session state for the client application.
 */
public class AppContext {

    private static AppContext instance;
    private ServerConnection connection;
    private UserDto currentUser;

    private AppContext() {
        this.connection = new ServerConnection();
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public ServerConnection getConnection() { return connection; }
    public UserDto getCurrentUser() { return currentUser; }
    public void setCurrentUser(UserDto user) { this.currentUser = user; }
    public boolean isLoggedIn() { return currentUser != null; }

    public void logout() {
        currentUser = null;
    }
}
