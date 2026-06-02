package com.auction.service;

public class AuctionException extends Exception {
    public AuctionException(String message) { super(message); }
    public AuctionException(String message, Throwable cause) { super(message, cause); }
}
