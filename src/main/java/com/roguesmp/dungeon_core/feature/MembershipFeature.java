package com.roguesmp.dungeon_core.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonDefinition;
import com.roguesmp.dungeon_core.DungeonFeature;
import com.roguesmp.dungeon_core.DungeonInstance;
import com.roguesmp.dungeon_core.DungeonManager;
import com.roguesmp.dungeon_core.FeatureContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who may join a run. With {@code one_per_player} a player belongs to at most one run of the dungeon; with
 * {@code invites} the owner can invite players, who have {@code invite_seconds} to accept. Pending invites are not saved.
 * Rule violations throw {@link IllegalStateException} with a player-facing message.
 */
public record MembershipFeature(boolean onePerPlayer, boolean invites, int inviteSeconds) implements DungeonFeature<MembershipFeature.State> {

    public static final String TYPE_KEY = "membership";

    public static final class State {
        public static final Codec<State> CODEC = Codec.unit(State::new);

        private final Map<UUID, Long> invites = new HashMap<>();
    }

    public static final Codec<MembershipFeature> CODEC = Codec.composite(
            Codec.BOOLEAN.optionalFieldOf("one_per_player", true).forGetter(MembershipFeature::onePerPlayer),
            Codec.BOOLEAN.optionalFieldOf("invites", true).forGetter(MembershipFeature::invites),
            Codec.INT.optionalFieldOf("invite_seconds", 60).forGetter(MembershipFeature::inviteSeconds),
            MembershipFeature::new
    );

    /**
     * Checks a new run of {@code dungeonId} for {@code ownerId}, before it is created.
     */
    public static void checkCreate(DungeonManager manager, DungeonDefinition definition, String dungeonId, UUID ownerId) {
        boolean onePerPlayer = definition.features().values().stream()
                .anyMatch(feature -> feature instanceof MembershipFeature membership && membership.onePerPlayer());
        if (onePerPlayer && manager.findFor(ownerId, dungeonId) != null) throw new IllegalStateException("Bạn đã có phó bản này rồi.");
    }

    public void invite(FeatureContext<State> context, UUID inviterId, UUID inviteeId) {
        DungeonInstance instance = context.instance();
        if (!invites) throw new IllegalStateException("Phó bản này không cho phép mời người khác.");
        if (!instance.ownerId().equals(inviterId)) throw new IllegalStateException("Chỉ chủ phó bản mới có thể mời người khác.");
        if (instance.isMember(inviteeId)) throw new IllegalStateException("Người này đã ở trong phó bản rồi.");
        checkJoin(context, inviteeId);

        context.state().invites.put(inviteeId, System.currentTimeMillis() + inviteSeconds * 1000L);
    }

    public void acceptInvite(FeatureContext<State> context, UUID inviteeId) {
        if (context.instance().isMember(inviteeId)) return;

        Long expiresAt = context.state().invites.remove(inviteeId);
        if (expiresAt == null || expiresAt < System.currentTimeMillis()) {
            throw new IllegalStateException("Bạn chưa được mời hoặc lời mời đã hết hạn.");
        }
        addMember(context, inviteeId);
    }

    public void addMember(FeatureContext<State> context, UUID playerId) {
        checkJoin(context, playerId);
        context.manager().addMember(context.instance(), playerId);
    }

    private void checkJoin(FeatureContext<State> context, UUID playerId) {
        DungeonInstance instance = context.instance();
        if (onePerPlayer && context.manager().findFor(playerId, instance.dungeonId()) != null) {
            throw new IllegalStateException("Người chơi này đã có phó bản này rồi.");
        }
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
