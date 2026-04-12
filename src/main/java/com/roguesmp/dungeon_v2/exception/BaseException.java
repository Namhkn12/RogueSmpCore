package com.roguesmp.dungeon_v2.exception;

public class BaseException extends RuntimeException {

    private final String userMessage;

    public BaseException(String message) {
        this(message, message, null);
    }

    public BaseException(String message, Throwable cause) {
        this(message, message, cause);
    }

    public BaseException(String message, String userMessage) {
        this(message, userMessage, null);
    }

    public BaseException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage == null || userMessage.isBlank() ? message : userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
