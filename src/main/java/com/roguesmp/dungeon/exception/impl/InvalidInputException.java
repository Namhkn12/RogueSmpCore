package com.roguesmp.dungeon.exception.impl;

import com.roguesmp.dungeon.exception.BaseException;

public class InvalidInputException extends BaseException {
    public InvalidInputException(String param, String reason, Throwable cause) {
        super("Invalid input param: " + param + " - " + reason, cause);
    }
}
