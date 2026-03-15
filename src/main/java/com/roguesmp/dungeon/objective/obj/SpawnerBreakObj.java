package com.roguesmp.dungeon.objective.obj;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.objective.IObjective;
import com.roguesmp.dungeon.objective.ObjectiveData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class SpawnerBreakObj implements IObjective {
    private final int requiredCount;
    private int brokenCount = 0;
    private boolean completed = false;

    public SpawnerBreakObj(ObjectiveData data) {
        Object raw = data.getParams().get("count");
        this.requiredCount = ((Number) raw).intValue();
    }

    @Override
    public void start(DungeonInstance instance, Party party) {
        party.getMembers().forEach(m -> {
            Player p = Bukkit.getPlayer(m);
            if(p != null){
                p.sendMessage("Mục tiêu: phá " + requiredCount + " spawner");
            }
        });
    }

    @Override
    public void process() {
        // SpawnerBreak không cần tick, để trống
        // Các objective như timer countdown sẽ dùng cái này
    }

    public void onSpawnerBreak(BlockBreakEvent e, DungeonInstance instance, Party party) {
        brokenCount++;
        party.getMembers().forEach(m -> {
            Player p = Bukkit.getPlayer(m);
            if(p != null){
                p.sendMessage("Đã phá spawner :" + brokenCount + "/" + requiredCount);
            }
        });
        if (brokenCount >= requiredCount) {
            completed = true;
            end(instance, party);
        }
    }

    @Override
    public void end(DungeonInstance instance, Party party) {
        instance.getActiveRoom().setCompleted(true);
        party.getMembers().forEach(m -> {
            Player p = Bukkit.getPlayer(m);
            if(p != null){
                p.sendMessage("Mục tiêu dungeon đã hoàn thành, hãy đi đến phòng tiếp theo");
            }
        });
    }

    @Override
    public boolean isCompleted() { return completed; }

    @Override
    public ObjectiveData getData() {
        return null;
    }
}