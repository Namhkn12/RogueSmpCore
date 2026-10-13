package com.roguesmp.dungeon_core.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonFeature;
import com.roguesmp.dungeon_core.DungeonInstance;
import com.roguesmp.dungeon_core.DungeonManager;
import com.roguesmp.dungeon_core.FeatureContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MembershipFeature(boolean invites, int inviteSeconds, boolean abandon) implements DungeonFeature<MembershipFeature.State> {

    public static final String TYPE_KEY = "membership";

    public static final class State {
        public static final Codec<State> CODEC = Codec.unit(State::new);

        private final Map<UUID, Long> invites = new HashMap<>();
    }

    public static final Codec<MembershipFeature> CODEC = Codec.composite(
            Codec.BOOLEAN.optionalFieldOf("invites", true).forGetter(MembershipFeature::invites),
            Codec.INT.optionalFieldOf("invite_seconds", 60).forGetter(MembershipFeature::inviteSeconds),
            Codec.BOOLEAN.optionalFieldOf("abandon", true).forGetter(MembershipFeature::abandon),
            MembershipFeature::new
    );

    public static List<DungeonInstance> invitesFor(DungeonManager manager, UUID playerId, String dungeonId) {
        long now = System.currentTimeMillis();
        return manager.instances().stream()
                .filter(instance -> instance.dungeonId().equals(dungeonId))
                .filter(instance -> {
                    FeatureContext<State> membership = manager.feature(instance, MembershipFeature.class);
                    if (membership == null) return false;
                    Long expiresAt = membership.state().invites.get(playerId);
                    return expiresAt != null && expiresAt >= now;
                })
                .toList();
    }

    public void invite(FeatureContext<State> context, UUID inviterId, UUID inviteeId) {
        DungeonInstance instance = context.instance();
        if (!invites) throw new IllegalStateException("Phó bản này không cho phép mời người khác.");
        if (!instance.ownerId().equals(inviterId)) throw new IllegalStateException("Chỉ chủ phó bản mới có thể mời người khác.");
        if (instance.isMember(inviteeId)) throw new IllegalStateException("Người này đã ở trong phó bản rồi.");
        if (context.manager().findFor(inviteeId, instance.dungeonId()) != null) throw new IllegalStateException("Người này đã có phó bản này rồi.");

        context.state().invites.put(inviteeId, System.currentTimeMillis() + inviteSeconds * 1000L);
    }

    public void acceptInvite(FeatureContext<State> context, UUID inviteeId) {
        if (context.instance().isMember(inviteeId)) return;

        Long expiresAt = context.state().invites.remove(inviteeId);
        if (expiresAt == null || expiresAt < System.currentTimeMillis()) {
            throw new IllegalStateException("Bạn chưa được mời hoặc lời mời đã hết hạn.");
        }
        context.manager().addMember(context.instance(), inviteeId);
    }

    public void abandon(FeatureContext<State> context, UUID playerId) {
        DungeonInstance instance = context.instance();
        if (!abandon) throw new IllegalStateException("Không thể rời bỏ phó bản này.");
        if (!instance.isMember(playerId)) throw new IllegalStateException("Bạn không phải thành viên của phó bản này.");

        if (instance.ownerId().equals(playerId)) context.end("đã bị chủ phó bản huỷ");
        else context.manager().removeMember(instance, playerId);
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public Codec<State> stateCodec() {
        return State.CODEC;
    }

    @Override
    public void onTick(FeatureContext<State> context) {
        long now = System.currentTimeMillis();
        context.state().invites.values().removeIf(expiresAt -> expiresAt < now);
    }
}
