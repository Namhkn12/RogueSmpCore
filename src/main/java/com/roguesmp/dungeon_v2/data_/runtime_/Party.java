package com.roguesmp.dungeon_v2.data_.runtime_;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Party {

    private UUID partyId;
    private UUID owner;
    private List<UUID> members;     // bao gồm cả owner
    private int maxSize;
    private boolean locked;         // đổi tên từ "status" → rõ nghĩa hơn

    // ── Constructors ──────────────────────────────────────────────────────────

    public Party() {}

    /**
     * @param partyId  ID duy nhất của party
     * @param owner    UUID của người tạo / chủ party
     * @param maxSize  số thành viên tối đa
     */
    public Party(UUID partyId, UUID owner, int maxSize) {
        this.partyId = partyId;
        this.owner   = owner;
        this.members = new ArrayList<>(List.of(owner)); // owner tự động là thành viên đầu tiên
        this.maxSize = maxSize;
        this.locked  = false;
    }

    // ── Domain methods ────────────────────────────────────────────────────────

    /** Thêm thành viên vào party. Trả về {@code false} nếu full hoặc đã có. */
    public boolean addMember(UUID playerId) {
        if (isFull() || members.contains(playerId)) return false;
        members.add(playerId);
        return true;
    }

    /** Xoá thành viên khỏi party. Trả về {@code false} nếu không tìm thấy. */
    public boolean removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    /** Chuyển quyền owner sang thành viên khác. */
    public boolean transferOwnership(UUID newOwner) {
        if (!members.contains(newOwner)) return false;
        this.owner = newOwner;
        return true;
    }

    /** Khoá party lại (dùng khi bắt đầu dungeon). */
    public void lock() { this.locked = true; }

    /** Mở party (dùng khi dungeon kết thúc hoặc huỷ). */
    public void unlock() { this.locked = false; }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isOwner(UUID playerId) {
        return owner.equals(playerId);
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public boolean isLocked() { return locked; }

    public boolean isOpen() { return !locked && !isFull(); }

    public int currentSize() { return members.size(); }

    /** Trả về view bất biến để tránh external mutation. */
    public List<UUID> getMembers() {
        return Collections.unmodifiableList(members);
    }

    // ── Getters / Setters (cần cho Gson deserialize) ──────────────────────────

    public UUID getPartyId() { return partyId; }
    public void setPartyId(UUID partyId) { this.partyId = partyId; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    /** Setter dùng cho Gson — dùng {@link #addMember}/{@link #removeMember} trong code thường. */
    public void setMembers(List<UUID> members) { this.members = new ArrayList<>(members); }

    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    /** @deprecated Dùng {@link #isLocked()} thay thế. Giữ lại để không break code cũ. */
    @Deprecated
    public boolean isStatus() { return locked; }

    /** @deprecated Dùng {@link #lock()}/{@link #unlock()} thay thế. */
    @Deprecated
    public void setStatus(boolean status) { this.locked = status; }

    @Override
    public String toString() {
        return "Party{partyId=" + partyId
                + ", owner=" + owner
                + ", members=" + members
                + ", maxSize=" + maxSize
                + ", locked=" + locked + '}';
    }
}