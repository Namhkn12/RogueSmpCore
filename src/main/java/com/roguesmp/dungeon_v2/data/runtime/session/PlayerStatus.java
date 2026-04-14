package com.roguesmp.dungeon_v2.data.runtime.session;

import com.roguesmp.dungeon_v2.helper.SerializableLocation;

public class PlayerStatus{
    public enum Status { PLAYING, DEAD, DISCONNECT, DEAD_DISCONNECT, OUT  }

    private int deadCount = 0;
    private Status status;
    private SerializableLocation checkPoint;

    public PlayerStatus(int deadCount, Status status, SerializableLocation checkPoint) {
        this.deadCount = deadCount;
        this.status = status;
        this.checkPoint = checkPoint;
    }

    public int getDeadCount() {
        return deadCount;
    }

    public void setDeadCount(int deadCount) {
        this.deadCount = deadCount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void upDead(){
        this.deadCount = deadCount + 1;
    }

    public SerializableLocation getCheckPoint() {
        return checkPoint;
    }

    public void setCheckPoint(SerializableLocation checkPoint) {
        this.checkPoint = checkPoint;
    }
}