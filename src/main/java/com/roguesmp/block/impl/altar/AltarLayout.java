package com.roguesmp.block.impl.altar;

import com.roguesmp.block.BlockPos;

final class AltarLayout {

    private static final int RADIUS = 3;
    private static final double SLOT_ANGLE = 2 * Math.PI / AltarSideSlot.values().length;

    private AltarLayout() {
    }

    static BlockPos positionOf(BlockPos center, AltarSideSlot slot) {
        double angle = slot.ordinal() * SLOT_ANGLE;
        return center.offset((int) Math.round(RADIUS * Math.cos(angle)), 0, (int) Math.round(RADIUS * Math.sin(angle)));
    }
}
