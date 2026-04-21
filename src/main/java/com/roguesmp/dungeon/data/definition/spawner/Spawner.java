package com.roguesmp.dungeon.data.definition.spawner;

import com.roguesmp.dungeon.data.definition.spawner.behavior.BehaviorData;

import java.util.List;
import java.util.Map;

public class Spawner {
    private String id;
    private String name;
    private Map<String, Integer> mobs;
    private int delay;
    private int minDelay;
    private int maxDelay;
    private int activeRange;
    private int spawnRange;
    private int maxNearBy;
    private int spawnCount;
    private List<BehaviorData> behaviors;

    public Spawner() {
    }

    public Spawner(String id, String name,Map<String, Integer> mobs, int delay, int minDelay, int maxDelay, int activeRange, int spawnRange, int maxNearBy, int spawnCount) {
        this.id = id;
        this.name = name;
        this.mobs = mobs;
        this.delay = delay;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.activeRange = activeRange;
        this.spawnRange = spawnRange;
        this.maxNearBy = maxNearBy;
        this.spawnCount = spawnCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Integer> getMobs() {
        return mobs;
    }

    public void setMobs(Map<String, Integer> mobs) {
        this.mobs = mobs;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getMinDelay() {
        return minDelay;
    }

    public void setMinDelay(int minDelay) {
        this.minDelay = minDelay;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
    }

    public int getActiveRange() {
        return activeRange;
    }

    public void setActiveRange(int activeRange) {
        this.activeRange = activeRange;
    }

    public int getSpawnRange() {
        return spawnRange;
    }

    public void setSpawnRange(int spawnRange) {
        this.spawnRange = spawnRange;
    }

    public int getMaxNearBy() {
        return maxNearBy;
    }

    public void setMaxNearBy(int maxNearBy) {
        this.maxNearBy = maxNearBy;
    }

    public int getSpawnCount() {
        return spawnCount;
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
    }

    public List<BehaviorData> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(List<BehaviorData> behaviors) {
        this.behaviors = behaviors;
    }
}