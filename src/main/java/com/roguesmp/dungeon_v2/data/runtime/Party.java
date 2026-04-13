package com.roguesmp.dungeon_v2.data.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Runtime party aggregate used to enter and own dungeon sessions.
 */
public class Party {

    private String partyId;
    private String owner;
    private List<String> members;
    private int maxSize;
    private boolean locked;
    private String instanceId;

    public Party() {
        this.members = new ArrayList<>();
    }

    public Party(String partyId, String owner, int maxSize) {
        this.partyId = partyId;
        this.owner = owner;
        this.members = new ArrayList<>(List.of(owner));
        this.maxSize = maxSize;
        this.locked = false;
        this.instanceId = null;
    }

    public boolean addMember(String playerId) {
        if (isFull() || members.contains(playerId)) {
            return false;
        }
        members.add(playerId);
        return true;
    }

    public boolean removeMember(String playerId) {
        return members.remove(playerId);
    }

    public boolean transferOwnership(String newOwner) {
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

    public boolean isMember(String playerId) {
        return members.contains(playerId);
    }

    public boolean isOwner(String playerId) {
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

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setMembers(List<String> members) {
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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
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
