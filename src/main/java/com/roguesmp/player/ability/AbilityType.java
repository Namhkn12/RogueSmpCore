package com.roguesmp.player.ability;

public enum AbilityType {
    PASSIVE("Bị động", 4),
    ACTIVE("Chủ động", 4),
    LIFELINE("Sinh tử", 1);

    private final String display;
    private final int maxSlots;

    AbilityType(String s, int maxSlots) {
        this.display = s;
        this.maxSlots = maxSlots;
    }

    public String getDisplay() {
        return display;
    }

    public int getMaxSlots() {
        return maxSlots;
    }
}
