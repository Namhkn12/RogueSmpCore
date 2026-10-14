package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.party.Party;
import com.roguesmp.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;
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

    @Override
    public DataResult<Void> prepare(Player creator, Set<UUID> members) {
        Party party = PartyManager.getInstance().getPartyByPlayer(creator);
        if (party == null) return DataResult.success(null);
        if (!party.isOwner(creator.getUniqueId().toString())) return DataResult.error("Chỉ trưởng nhóm mới có thể tạo phó bản cho cả nhóm.");
        if (party.getInstanceId() != null && !party.getInstanceId().isBlank()) return DataResult.error("Nhóm của bạn đang ở trong một phó bản khác.");

        party.getMembers().forEach(member -> members.add(UUID.fromString(member)));
        return DataResult.success(null);
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
        Party party = context.instance().members().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .map(PartyManager.getInstance()::getPartyByPlayer)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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
