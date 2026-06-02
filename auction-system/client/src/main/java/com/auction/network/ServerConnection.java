package com.auction.network;

import com.auction.dto.SocketMessage;
import com.auction.enums.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Fixed: dùng BlockingQueue để tách luồng đọc response và push message.
 * Tránh sendAndReceive() bị block vĩnh viễn.
 */
public class ServerConnection {

    private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ObjectMapper objectMapper;
    private volatile boolean connected = false;

    // Queue chứa SUCCESS/ERROR responses cho sendAndReceive()
    private final BlockingQueue<SocketMessage> responseQueue = new LinkedBlockingQueue<>();

    // Listeners cho push messages (BID_UPDATE, AUCTION_ENDED, ...)
    private final List<Consumer<SocketMessage>> pushListeners = new CopyOnWriteArrayList<>();

    public ServerConnection() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(0); // no timeout
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        startReaderThread();
        logger.info("Connected to server " + host + ":" + port);
    }

    /**
     * Gửi request và chờ response (timeout 10 giây).
     * Thread-safe: dùng synchronized để tránh 2 request gửi cùng lúc.
     */
    public synchronized SocketMessage sendAndReceive(SocketMessage request) throws IOException {
        if (!connected) throw new IOException("Not connected to server");

        // Xóa queue cũ trước khi gửi
        responseQueue.clear();

        String json = objectMapper.writeValueAsString(request);
        System.out.println("[NET] >> SEND: " + request.getType());
        out.println(json);
        out.flush();

        try {
            // Chờ tối đa 10 giây
            SocketMessage response = responseQueue.poll(10, TimeUnit.SECONDS);
            if (response == null) {
                throw new IOException("Timeout: server did not respond within 10 seconds");
            }
            System.out.println("[NET] << RECV: " + response.getType()
                    + (response.getError() != null ? " err=" + response.getError() : ""));
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for response");
        }
    }

    /**
     * Thread đọc tất cả messages từ server.
     * SUCCESS/ERROR → đưa vào responseQueue cho sendAndReceive().
     * PUSH messages → dispatch tới listeners.
     */
    private void startReaderThread() {
        Thread reader = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    try {
                        SocketMessage msg = objectMapper.readValue(line, SocketMessage.class);
                        System.out.println("[NET] << RAW: " + msg.getType());

                        MessageType type = msg.getType();
                        if (type == MessageType.SUCCESS || type == MessageType.ERROR
                                || type == MessageType.PONG) {
                            // Response cho sendAndReceive
                            responseQueue.put(msg);
                        } else {
                            // Push message từ server
                            dispatchPush(msg);
                        }
                    } catch (Exception e) {
                        System.out.println("[NET] Parse error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.out.println("[NET] Connection lost: " + e.getMessage());
                }
            } finally {
                connected = false;
                // Unblock sendAndReceive nếu đang chờ
                try {
                    SocketMessage err = SocketMessage.error("Connection closed");
                    responseQueue.put(err);
                } catch (InterruptedException ignored) {}
            }
        }, "server-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void dispatchPush(SocketMessage msg) {
        for (Consumer<SocketMessage> listener : pushListeners) {
            try { listener.accept(msg); }
            catch (Exception e) { logger.warning("Push listener error: " + e.getMessage()); }
        }
    }

    public void addPushListener(Consumer<SocketMessage> listener) {
        pushListeners.add(listener);
    }

    public void removePushListener(Consumer<SocketMessage> listener) {
        pushListeners.remove(listener);
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}
