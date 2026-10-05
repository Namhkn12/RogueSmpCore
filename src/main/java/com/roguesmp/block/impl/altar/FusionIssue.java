package com.roguesmp.block.impl.altar;

public enum FusionIssue {
    NONE(""),
    BUSY("Altar đang vận hành."),
    NO_INPUT("Hãy đặt vật phẩm chính lên altar trung tâm."),
    STRUCTURE_INCOMPLETE("Chưa đủ 8 altar phụ xung quanh."),
    NO_RECIPE("Không có công thức phù hợp.");

    private final String message;

    FusionIssue(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
