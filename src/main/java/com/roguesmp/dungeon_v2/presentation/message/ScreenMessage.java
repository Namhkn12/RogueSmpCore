package com.roguesmp.dungeon_v2.presentation.message;

public class ScreenMessage {

    public enum Type {
        TITLE,
        ACTIONBAR
    }

    private final Type type;
    private final String title;
    private final String subtitle;

    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    private final long delay;

    public ScreenMessage(Type type, String title, String subtitle,
                         int fadeIn, int stay, int fadeOut, long delay) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        this.delay = delay;
    }

    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getFadeIn() { return fadeIn; }
    public int getStay() { return stay; }
    public int getFadeOut() { return fadeOut; }
    public long getDelay() { return delay; }

    public static ScreenMessage title(String title, String sub) {
        return new ScreenMessage(Type.TITLE, title, sub, 10, 40, 10, 0);
    }

    public ScreenMessage delay(long delay) {
        return new ScreenMessage(type, title, subtitle, fadeIn, stay, fadeOut, delay);
    }
}