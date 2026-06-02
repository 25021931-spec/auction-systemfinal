package com.auction.dto;

import com.auction.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocketMessage {

    private MessageType type;
    private Object payload;
    private String error;
    private long timestamp;

    public SocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public SocketMessage(MessageType type, Object payload) {
        this();
        this.type = type;
        this.payload = payload;
    }

    public static SocketMessage success(Object payload) {
        return new SocketMessage(MessageType.SUCCESS, payload);
    }

    public static SocketMessage error(String errorMessage) {
        SocketMessage msg = new SocketMessage(MessageType.ERROR, null);
        msg.setError(errorMessage);
        return msg;
    }

    // Getters & Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
