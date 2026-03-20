package com.roguesmp.dungeon.instance;

import com.roguesmp.dungeon.behavior.IBehavior;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpawnerInstance {
        private UUID id;
        private Map<String, Integer> mobs;
        private List<IBehavior> behaviors;
}
