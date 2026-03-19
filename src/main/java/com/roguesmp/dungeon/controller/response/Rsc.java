package com.roguesmp.dungeon.controller.response;

public enum Rsc {
    SUCCESS(200, "Success"),
    CREATED(201, "Created successfully"),

    // Client errors
    BAD_REQUEST(400, "Bad request"),
    NOT_FOUND(404, "Resource not found"),
    UNAUTHORIZED(401, "Unauthorized access"),

    // Server errors
    INTERNAL_ERROR(500, "Internal server error");

    private final int code;
    private final String message;

    Rsc(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}