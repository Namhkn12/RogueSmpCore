package com.roguesmp.dungeon.repository.impl;

import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.repository.IRegionRepository;

import java.util.List;
import java.util.Map;

public class RegionRepository implements IRegionRepository {
    @Override
    public Map<String, List<Region>> loadAll() {
        return Map.of();
    }

    @Override
    public void save(Map<String, List<Region>> regions) {

    }
}
