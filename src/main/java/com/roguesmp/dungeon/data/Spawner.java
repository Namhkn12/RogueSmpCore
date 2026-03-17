package com.roguesmp.dungeon.data;

import java.util.Map;
import java.util.UUID;

public class Spawner {
    private UUID id;
    private Map<String, Integer> mobs;
    private int delay;
    private int minDelay;
    private int maxDelay;
    private int activeRange;
    private int spawnRange;
    private int maxNearBy;
    private int spawnCount;

}
