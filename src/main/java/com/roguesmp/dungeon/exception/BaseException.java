package com.roguesmp.dungeon.exception;

public class BaseException extends RuntimeException{
    public BaseException(String message){
        super(message);
    }
    public BaseException(String message, String reason, Throwable cause){
        super(message);
    }
    public BaseException(String message, Throwable cause){
        super(message);
    }


}
