package com.roguesmp.entity.boss.hollowheart;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CrystalPhase(
        String name,
        List<String> pool,
        @Nullable String introduced,
        boolean lastStand,
        List<String> hazards,
        int hazardMinTicks,
        int hazardMaxTicks,
        int exposeTicks,
        int restTicks,
        int shardCount,
        int shardTimerTicks,
        int guardianCount,
        int guardianTimerTicks,
        int floorWaves,
        int floorWarnTicks,
        int orbCount,
        double orbSpeed,
        int pulseCount,
        int pulseChargeTicks,
        int conduitCount,
        int overloadChargeTicks
) {

    public static final String SHARDS = "crystal_shards";
    public static final String FLOOR = "crystal_corrupted_floor";
    public static final String GUARDIANS = "crystal_guardians";
    public static final String ORBS = "energy_orbs";
    public static final String PULSE = "crystal_pulse";
    public static final String OVERLOAD = "crystal_overload";

    public static final List<CrystalPhase> PHASES = List.of(
            new CrystalPhase("Thức tỉnh", List.of(SHARDS, GUARDIANS), null, false, List.of(FLOOR), 600, 900,
                    200, 60, 4, 300, 3, 600, 3, 40, 3, 1.2, 2, 60, 3, 400),
            new CrystalPhase("Cộng hưởng", List.of(SHARDS, GUARDIANS, ORBS), ORBS, false, List.of(FLOOR), 400, 700,
                    200, 60, 5, 300, 4, 600, 3, 40, 3, 1.2, 2, 60, 3, 400),
            new CrystalPhase("Rạn nứt", List.of(SHARDS, GUARDIANS, ORBS), PULSE, false, List.of(FLOOR, PULSE), 300, 560,
                    200, 60, 5, 300, 4, 600, 3, 30, 3, 1.4, 2, 60, 3, 400),
            new CrystalPhase("Quá tải", List.of(SHARDS, GUARDIANS, ORBS, OVERLOAD), OVERLOAD, false, List.of(FLOOR, PULSE), 200, 400,
                    160, 40, 6, 300, 4, 600, 4, 30, 4, 1.4, 3, 50, 3, 400),
            new CrystalPhase("Trái tim vỡ", List.of(), null, true, List.of(), 0, 0,
                    0, 0, 6, 300, 2, 600, 4, 30, 4, 1.4, 1, 40, 3, 400)
    );
}
