package com.roguesmp.block.mining;

import com.roguesmp.block.BlockVisual;
import com.roguesmp.block.SmpBlock;

import java.util.UUID;

final class BreakSession {

    private final UUID playerId;
    private final SmpBlock block;
    private final int heldSlot;
    private final UUID crackOverlayId;
    private int elapsedTicks;
    private int shownStage = -1;

    BreakSession(UUID playerId, SmpBlock block, int heldSlot, UUID crackOverlayId) {
        this.playerId = playerId;
        this.block = block;
        this.heldSlot = heldSlot;
        this.crackOverlayId = crackOverlayId;
    }

    UUID playerId() {
        return playerId;
    }

    SmpBlock block() {
        return block;
    }

    int heldSlot() {
        return heldSlot;
    }

    /** {@code requiredTicks} is re-read live each tick, so it can change mid-dig (gear/effects). */
    boolean advance(int requiredTicks) {
        elapsedTicks++;
        return elapsedTicks >= requiredTicks;
    }

    boolean crackChanged(int requiredTicks) {
        int stage = Math.min((elapsedTicks * BlockVisual.CRACK_STAGES) / requiredTicks, BlockVisual.CRACK_STAGES - 1);
        if (stage == shownStage) return false;
        shownStage = stage;
        return true;
    }

    int shownStage() {
        return shownStage;
    }

    float crackProgress(int requiredTicks) {
        return Math.min(elapsedTicks / (float) requiredTicks, 1f);
    }

    UUID crackOverlayId() {
        return crackOverlayId;
    }
}
