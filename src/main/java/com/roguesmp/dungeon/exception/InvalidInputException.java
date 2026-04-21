package com.roguesmp.dungeon.exception;

public class InvalidInputException extends BaseException {
    public InvalidInputException(String param, String reason) {
        super("Invalid input param: " + param + " - " + reason, reason);
    }

    public InvalidInputException(String param, String reason, Throwable cause) {
        super("Invalid input param: " + param + " - " + reason, reason, cause);
    }
}
