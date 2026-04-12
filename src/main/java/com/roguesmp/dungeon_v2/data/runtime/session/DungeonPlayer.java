package com.roguesmp.dungeon_v2.data.runtime.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonPlayer {
    private Map<UUID, PlayerStatus> players = new HashMap<>();

    public DungeonPlayer() {
    }

    public DungeonPlayer(Map<UUID, PlayerStatus> players) {
        this.players = players;
    }

    public Map<UUID, PlayerStatus> getPlayers() {
        return players;
    }

    public void setPlayers(Map<UUID, PlayerStatus> players) {
        this.players = players;
    }

    public void addPlayerStatus(UUID pid, PlayerStatus status){
        players.put(pid, status);
    }

    public void setPlayerStatus(UUID pid, PlayerStatus.Status status){
        PlayerStatus playerStatus = players.get(pid);
        playerStatus.setStatus(status);
    }

    public void countPlayerDead(UUID pid){
        PlayerStatus playerStatus = players.get(pid);
        playerStatus.upDead();
    }
}
