package com.auction;

import com.auction.network.AuctionServer;

import java.io.IOException;
import java.util.logging.Logger;

public class ServerMain {

    private static final Logger logger = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) {
        int port = AuctionServer.DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { logger.warning("Invalid port, using default " + port); }
        }

        AuctionServer server = new AuctionServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        try {
            server.start();
        } catch (IOException e) {
            logger.severe("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}
