package com.auction.enums;

public enum MessageType {
    // Auth
    REGISTER, LOGIN, LOGOUT,
    // Auction
    CREATE_AUCTION, GET_AUCTIONS, GET_AUCTION_DETAIL,
    UPDATE_AUCTION, DELETE_AUCTION,
    // Bidding
    PLACE_BID, AUTO_BID, CANCEL_AUTO_BID,
    // Realtime push from server
    BID_UPDATE, AUCTION_ENDED, AUCTION_EXTENDED,
    // Admin
    GET_ALL_USERS, BAN_USER,
    // Response
    SUCCESS, ERROR,
    // Heartbeat
    PING, PONG
}
