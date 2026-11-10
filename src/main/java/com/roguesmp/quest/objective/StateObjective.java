package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class StateObjective implements QuestObjective {

    public static final class Progress implements ObjectiveProgress {

        public static Codec<Progress> codec(String typeId) {
            return Codec.composite(
                    Codec.BOOLEAN.optionalFieldOf("met", false).forGetter(Progress::isMet),
                    met -> new Progress(typeId, met)
            );
        }

        private final String typeId;
        private boolean met;

        public Progress(String typeId, boolean met) {
            this.typeId = typeId;
            this.met = met;
        }

        public boolean isMet() {
            return met;
        }

        @Override
        public boolean isDefault() {
            return !met;
        }

        @Override
        public String getTypeId() {
            return typeId;
        }
    }

    protected abstract boolean test(SmpPlayer smpPlayer);

    protected abstract String label();

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        String mark = isCompleted(progress) ? " <green>✔" : "";
        return List.of(Utils.fromString("<gray>" + label() + mark));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString("<gray>" + label()));
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        return ((Progress) progress).met;
    }

    @Override
    public ObjectiveProgress createNewProgress() {
        return new Progress(getTypeId(), false);
    }

    @Override
    public boolean onRefresh(SmpPlayer smpPlayer, ObjectiveProgress progress) {
        if (!(progress instanceof Progress state) || state.met || !test(smpPlayer)) return false;
        state.met = true;
        return true;
    }
}
