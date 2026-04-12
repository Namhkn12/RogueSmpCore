package com.roguesmp.dungeon_v2.data.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Runtime party aggregate used to enter and own dungeon sessions.
 */
public class Party {

    private UUID partyId;
    private UUID owner;
    private List<UUID> members;
    private int maxSize;
    private boolean locked;
    private UUID instanceId;

    public Party() {
        this.members = new ArrayList<>();
    }

    public Party(UUID partyId, UUID owner, int maxSize) {
        this.partyId = partyId;
        this.owner = owner;
        this.members = new ArrayList<>(List.of(owner));
        this.maxSize = maxSize;
        this.locked = false;
        this.instanceId = null;
    }

    public boolean addMember(UUID playerId) {
        if (isFull() || members.contains(playerId)) {
            return false;
        }
        members.add(playerId);
        return true;
    }

    public boolean removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    public boolean transferOwnership(UUID newOwner) {
        if (!members.contains(newOwner)) {
            return false;
        }
        this.owner = newOwner;
        return true;
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isOwner(UUID playerId) {
        return owner != null && owner.equals(playerId);
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isOpen() {
        return !locked && !isFull();
    }

    public int currentSize() {
        return members.size();
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public List<UUID> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setMembers(List<UUID> members) {
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId) {
        this.instanceId = instanceId;
    }

    @Deprecated
    public boolean isStatus() {
        return locked;
    }

    @Deprecated
    public void setStatus(boolean status) {
        this.locked = status;
    }

    @Override
    public String toString() {
        return "Party{partyId=" + partyId
                + ", owner=" + owner
                + ", members=" + members
                + ", maxSize=" + maxSize
                + ", locked=" + locked + '}';
    }
}
