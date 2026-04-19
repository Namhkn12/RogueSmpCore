package com.roguesmp.dungeon_v2.data.runtime.session;

import java.util.HashMap;
import java.util.Map;

public class DungeonPlayer {
    private Map<String, PlayerStatus> players = new HashMap<>();

    public DungeonPlayer() {
    }

    public DungeonPlayer(Map<String, PlayerStatus> players) {
        this.players = players;
    }

    public Map<String, PlayerStatus> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, PlayerStatus> players) {
        this.players = players != null ? players : new HashMap<>();
    }

    public void addPlayerStatus(String pid, PlayerStatus status){
        players.put(pid, status);
    }

    public void setPlayerStatus(String pid, PlayerStatus.Status status){
        PlayerStatus playerStatus = players.get(pid);
        if (playerStatus == null) return;
        playerStatus.setStatus(status);
    }

    public void countPlayerDead(String pid){
        PlayerStatus playerStatus = players.get(pid);
        if (playerStatus == null) return;
        playerStatus.upDead();
    }
}
