package com.roguesmp.dungeon.data;

import java.util.List;
import java.util.UUID;

public class Party {
    private UUID partyId;
    private UUID owner;
    private List<UUID> members;
    private int size;
    private boolean status;

    public Party() {
    }

    public Party(UUID partyId, UUID owner, List<UUID> members, int size, boolean status) {
        this.partyId = partyId;
        this.owner = owner;
        this.members = members;
        this.size = size;
        this.status = status;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public int getSize() {
        return size;
    }

    public boolean isStatus() {
        return status;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void setMembers(List<UUID> members) {
        this.members = members;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Party{" +
                "partyId=" + partyId +
                ", owner=" + owner +
                ", members=" + members +
                ", size=" + size +
                ", status=" + status +
                '}';
    }
}
