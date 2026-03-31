package com.roguesmp.dungeon_v2.data_.objective_.impl_;

import com.roguesmp.dungeon_v2.data_.objective_.BaseObjective;
import com.roguesmp.dungeon_v2.data_.objective_.IProgressable;
import com.roguesmp.dungeon_v2.data_.objective_.byevent_.IBlockBreakAware;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class SpawnerBreaker extends BaseObjective implements IProgressable, IBlockBreakAware {
    public static final String TYPE = "spawner_breaker";
    private int require = 3;
    private int count = 0;

    public void setRequire(int require) { this.require = require; }

    @Override
    public void start() {

    }

    @Override
    public void finish() {

    }

    @Override
    public void progress() {
        count++;
        if(count >= require){
            complete();
        }
    }

    @Override
    public void onBlockBreak(Material type) {
        if(type == Material.SPAWNER){
            progress();
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("require", require);
        data.put("count", count);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.require = ((Number) data.getOrDefault("require", 3)).intValue();
        this.count   = ((Number) data.getOrDefault("count",    0)).intValue();
    }
}
