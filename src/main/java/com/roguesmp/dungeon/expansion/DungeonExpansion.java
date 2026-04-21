package com.roguesmp.dungeon.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonExpansion extends PlaceholderExpansion {

    private final Map<UUID, String> timeMap    = new ConcurrentHashMap<>();
    private final Map<UUID, String> scoreMap   = new ConcurrentHashMap<>();
    private final Map<UUID, String> nameMap    = new ConcurrentHashMap<>();
    private final Map<UUID, String[]> objMap   = new ConcurrentHashMap<>();
    private final Map<UUID, String[]> memberMap = new ConcurrentHashMap<>();

    @Override public String getIdentifier() { return "dungeon"; }
    @Override public String getAuthor()     { return "RogueSMP"; }
    @Override public String getVersion()    { return "1.0"; }
    @Override public boolean persist()      { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        UUID id = player.getUniqueId();

        return switch (params) {
            case "name"     -> nameMap.getOrDefault(id, "");
            case "time"     -> timeMap.getOrDefault(id, "");
            case "score"    -> scoreMap.getOrDefault(id, "");
            case "obj_0"    -> getArr(objMap, id, 0);
            case "obj_1"    -> getArr(objMap, id, 1);
            case "obj_2"    -> getArr(objMap, id, 2);
            case "obj_3"    -> getArr(objMap, id, 3);
            case "member_0" -> getArr(memberMap, id, 0);
            case "member_1" -> getArr(memberMap, id, 1);
            case "member_2" -> getArr(memberMap, id, 2);
            case "member_3" -> getArr(memberMap, id, 3);
            default         -> "";
        };
    }

    private String getArr(Map<UUID, String[]> map, UUID id, int idx) {
        String[] arr = map.get(id);
        return (arr != null && idx < arr.length) ? arr[idx] : "";
    }

    // ─── Setters ───────────────────────────────────────────
    public void setName(UUID id, String val)         { nameMap.put(id, val); }
    public void setTime(UUID id, String val)         { timeMap.put(id, val); }
    public void setScore(UUID id, String val)        { scoreMap.put(id, val); }
    public void setObjectives(UUID id, String[] val) { objMap.put(id, val); }
    public void setMembers(UUID id, String[] val)    { memberMap.put(id, val); }

    public void clear(UUID id) {
        nameMap.remove(id);
        timeMap.remove(id);
        scoreMap.remove(id);
        objMap.remove(id);
        memberMap.remove(id);
    }
}