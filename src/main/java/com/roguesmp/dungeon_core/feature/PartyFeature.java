package com.roguesmp.dungeon_core.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonDefinition;
import com.roguesmp.dungeon_core.DungeonFeature;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon_party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record PartyFeature() implements DungeonFeature<PartyFeature.State> {

    public static final String TYPE_KEY = "party";

    public static final class State {
        public static final Codec<State> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("party", "").forGetter(state -> state.partyId),
                State::new
        );

        private String partyId;

        public State(String partyId) {
            this.partyId = partyId;
        }
    }

    public static final Codec<PartyFeature> CODEC = Codec.unit(PartyFeature::new);

    public static Set<UUID> startingMembers(DungeonDefinition definition, Player owner) {
        if (definition.features().values().stream().noneMatch(PartyFeature.class::isInstance)) return Set.of();

        Party party = PartyManager.getInstance().getPartyByPlayer(owner);
        if (party == null) throw new IllegalStateException("Phó bản này cần một nhóm, hãy tạo nhóm bằng /party create.");
        if (!party.isOwner(owner.getUniqueId().toString())) throw new IllegalStateException("Chỉ trưởng nhóm mới có thể bắt đầu phó bản.");
        if (party.getInstanceId() != null && !party.getInstanceId().isBlank()) throw new IllegalStateException("Nhóm của bạn đang ở trong một phó bản khác.");

        Set<UUID> members = new LinkedHashSet<>();
        party.getMembers().forEach(member -> members.add(UUID.fromString(member)));
        return members;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public State createState() {
        return new State("");
    }

    @Override
    public Codec<State> stateCodec() {
        return State.CODEC;
    }

    @Override
    public void onStart(FeatureContext<State> context) {
        Player owner = Bukkit.getPlayer(context.instance().ownerId());
        Party party = owner == null ? null : PartyManager.getInstance().getPartyByPlayer(owner);
        if (party == null) return;
        party.setInstanceId(context.instance().id().toString());
        context.state().partyId = party.getPartyId();
    }

    @Override
    public void onEnd(FeatureContext<State> context) {
        Party party = context.state().partyId.isEmpty() ? null : PartyManager.getInstance().getPartyById(context.state().partyId);
        if (party != null && context.instance().id().toString().equals(party.getInstanceId())) party.setInstanceId(null);
    }
}
