package com.roguesmp.dungeon.dto;

public class ActionResult<T> {

    public enum Status {
        OK,
        CREATED,
        NOT_FOUND,
        INVALID,
        UNAUTHORIZED,
        FAILED
    }

    private final Status status;
    private final String message;
    private final T data;

    private ActionResult(Status status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // --- Factory methods ---

    public static <T> ActionResult<T> ok(T data) {
        return new ActionResult<>(Status.OK, null, data);
    }

    public static <T> ActionResult<T> ok(String message, T data) {
        return new ActionResult<>(Status.OK, message, data);
    }

    public static <T> ActionResult<T> ok(String message) {
        return new ActionResult<>(Status.OK, message, null);
    }

    public static <T> ActionResult<T> created(String message, T data) {
        return new ActionResult<>(Status.CREATED, message, data);
    }

    public static <T> ActionResult<T> notFound(String message) {
        return new ActionResult<>(Status.NOT_FOUND, message, null);
    }

    public static <T> ActionResult<T> invalid(String message) {
        return new ActionResult<>(Status.INVALID, message, null);
    }

    public static <T> ActionResult<T> unauthorized(String message) {
        return new ActionResult<>(Status.UNAUTHORIZED, message, null);
    }

    public static <T> ActionResult<T> failed(String message) {
        return new ActionResult<>(Status.FAILED, message, null);
    }

    /* Helper */

    public boolean isOk() {
        return status == Status.OK || status == Status.CREATED;
    }

    public boolean hasData() {
        return data != null;
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}