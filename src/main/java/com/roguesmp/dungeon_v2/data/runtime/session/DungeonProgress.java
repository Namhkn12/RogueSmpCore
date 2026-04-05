package com.roguesmp.dungeon_v2.data.runtime.session;

import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable progress state of one dungeon run.
 *
 * roomSequence   — thứ tự roomId được roll lúc tạo instance.
 * clearedRoomIds — history các room đã hoàn thành.
 * currentRoom    — room đang active, chứa objectiveStates để restore.
 */
public class DungeonProgress {

    public enum Status { IN_PROGRESS, COMPLETED, FAILED, ABANDONED }

    private int clearedRooms;
    private int minimumRooms;
    private Status status;

    private int score;
    private SerializableLocation checkpoint;
    private RoomInstance currentRoom;
    private List<String> roomPool;
    private List<String> nextRooms;
    private List<String> clearedRoomIds;

    public DungeonProgress() {
        this.status = Status.IN_PROGRESS;
        this.roomPool = new ArrayList<>();
        this.clearedRoomIds = new ArrayList<>();
        this.clearedRooms = 0;
    }

    public DungeonProgress(int minimumRooms, List<String> roomSequence) {
        this();
        this.minimumRooms = minimumRooms;
        this.roomPool = new ArrayList<>(roomSequence);
    }

    /**
     * Đánh dấu currentRoom là xong, đưa vào history, tăng counter.
     */
    public void markCurrentRoomCleared() {
        if (currentRoom == null) return;
        currentRoom.setCompleted(true);
        clearedRoomIds.add(currentRoom.getRoomId());
        clearedRooms++;
    }

    public boolean isBossUnlocked() {
        return clearedRooms >= minimumRooms;
    }

    public int getClearedRooms() { return clearedRooms; }
    public void setClearedRooms(int clearedRooms) { this.clearedRooms = clearedRooms; }

    public int getMinimumRooms() { return minimumRooms; }
    public void setMinimumRooms(int minimumRooms) { this.minimumRooms = minimumRooms; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public RoomInstance getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(RoomInstance currentRoom) { this.currentRoom = currentRoom; }

    public List<String> getRoomPool() { return roomPool; }
    public void setRoomPool(List<String> roomPool) { this.roomPool = roomPool; }

    public List<String> getClearedRoomIds() { return clearedRoomIds; }
    public void setClearedRoomIds(List<String> clearedRoomIds) { this.clearedRoomIds = clearedRoomIds; }

    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score = score;
    }

    public SerializableLocation getCheckpoint() {
        return checkpoint;
    }
    public void setCheckpoint(SerializableLocation checkpoint) {
        this.checkpoint = checkpoint;
    }
}