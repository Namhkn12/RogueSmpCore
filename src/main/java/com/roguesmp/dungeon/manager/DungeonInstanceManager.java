package com.roguesmp.dungeon.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import com.google.gson.reflect.TypeToken;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class DungeonInstanceManager {

    private final Map<UUID, DungeonInstance> dgInstances = new HashMap<>();
    private final Gson gson;

    public void init(RogueSmpCore plugin){
    }

    public DungeonInstanceManager(Gson gson) {
        this.gson = gson;
    }

    public void load() {
        // load instance from file - restore instance after server close
    }

    public void save() {
        // save instance
    }

    public DungeonInstance createInstance(
            String dungeonId,
            UUID partyId,
            Location location
    ) {


        DungeonInstance instance = new DungeonInstance(
                dungeonId,
                partyId,
                System.currentTimeMillis(),
                location,
                true,
                0.0
        );

        dgInstances.put(instance.getUuid(), instance);

        return instance;
    }

    public void deleteDungeonInstance(String instanceId) {
        dgInstances.remove(instanceId);
    }

    public DungeonInstance getDungeonInstance(String instanceId) {
        return dgInstances.get(instanceId);
    }

    public Collection<DungeonInstance> getAllInstances() {
        return dgInstances.values();
    }

    public boolean exists(String instanceId) {
        return dgInstances.containsKey(instanceId);
    }
}