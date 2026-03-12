package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Region;

import java.util.List;
import java.util.Map;

public interface IRegionRepository {
    Map<String, List<Region>> loadAll();
    void save(Map<String, List<Region>> regions);
}
