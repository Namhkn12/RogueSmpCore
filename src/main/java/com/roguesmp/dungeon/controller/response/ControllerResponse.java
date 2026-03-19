package com.roguesmp.dungeon.controller.response;

public class ControllerResponse<T> {

    private final boolean success;
    private Rsc code;
    private final String message;
    private final T data;

    private ControllerResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    private ControllerResponse(Rsc code, T data) {
        this.code = code;
        this.data = data;
        this.success = true;
        this.message = "";
    }

    public static <T> ControllerResponse<T> success(String message, T data) {
        return new ControllerResponse<>(true, message, data);
    }

    public static <T> ControllerResponse<T> success(String message) {
        return new ControllerResponse<>(true, message, null);
    }

    public static <T> ControllerResponse<T> failure(String message) {
        return new ControllerResponse<>(false, message, null);
    }

    public static <T> ControllerResponse<T> response(Rsc code, T data){
        return new ControllerResponse<>(code, data);
    }

    public static <T> ControllerResponse<T> response(Rsc code){
        return new ControllerResponse<>(code, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
