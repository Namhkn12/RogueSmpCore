package com.roguesmp.dungeon_v2.data_.objective_.impl_;

import com.roguesmp.dungeon_v2.data_.objective_.BaseObjective;
import com.roguesmp.dungeon_v2.data_.objective_.IProgressable;
import com.roguesmp.dungeon_v2.data_.objective_.byevent_.IEntityKillAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonsterHunter extends BaseObjective implements IProgressable, IEntityKillAware {

    public static final String TYPE = "monster_hunter";

    private int require = 1;
    private int count = 0;
    private List<String> targetIds = List.of();

    public void setRequire(int require) { this.require = require; }
    public void setTargetIds(List<String> ids) { this.targetIds = ids; }

    @Override
    public void start() {

    }

    @Override
    public void finish() {

    }

    @Override
    public void progress() {
        count++;

        if (count >= require && !completed) {
            complete();
        }
    }

    @Override
    public void onEntityKilled(String mobId) {
        if(isValidTarget(mobId)){
            progress();
        }
    }

    private boolean isValidTarget(String mobId) {
        if (targetIds == null || targetIds.isEmpty()) return true;
        return targetIds.contains(mobId);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("require", require);
        data.put("count", count);
        data.put("targets", targetIds);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.require = ((Number) data.getOrDefault("require", 1)).intValue();
        this.count   = ((Number) data.getOrDefault("count",    0)).intValue();
        this.targetIds = (List<String>) data.getOrDefault("targets", List.of());
    }
}
