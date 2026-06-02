package com.auction;

/**
 * Launcher tách biệt để tránh lỗi JavaFX module khi chạy từ classpath thông thường.
 * IntelliJ nên set Main class = com.auction.MainLauncher
 */
public class MainLauncher {
    public static void main(String[] args) {
        ClientMain.main(args);
    }
}
