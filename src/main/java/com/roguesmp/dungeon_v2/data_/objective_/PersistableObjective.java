package com.roguesmp.dungeon_v2.data_.objective_;

import java.util.Map;

public interface PersistableObjective {
    Map<String, Object> serialize();
    void deserialize(Map<String, Object> data);
}
