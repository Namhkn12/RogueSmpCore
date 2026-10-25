package com.roguesmp.quest.objective;

import com.roguesmp.quest.QuestManager;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.goal.IntRange;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Counts named signals sent by code through {@link com.roguesmp.quest.QuestManager#signal}, so other systems can drive
 * quests without the quest system knowing about them.
 */
public class SignalObjective implements QuestObjective {

    public static final String TYPE_KEY = "signal";

    public static final class Progress implements ObjectiveProgress {
        public static final Codec<Progress> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("count", 0).forGetter(Progress::getCount),
                Progress::new
        );

        private int count;
        private int goal;

        public Progress(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public int getGoal() {
            return goal;
        }

        @Override
        public boolean isDefault() {
            return count == 0;
        }

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public static final Codec<SignalObjective> CODEC = Codec.composite(
            Codec.STRING.fieldOf("signal").forGetter(SignalObjective::getSignal),
            IntRange.CODEC.optionalFieldOf("amount", IntRange.exactly(1)).forGetter(SignalObjective::getAmount),
            Codec.STRING.optionalFieldOf("display", "").forGetter(SignalObjective::getDisplay),
            SignalObjective::new
    );

    private final String signal;
    private final IntRange amount;
    private final String display;

    public SignalObjective(String signal, IntRange amount, String display) {
        this.signal = signal;
        this.amount = amount;
        this.display = display;
    }

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        String countColor = isCompleted(progress) ? "<gold>" : "<yellow>";
        return List.of(Utils.fromString(getLabel() + " <gray>(</gray>" + countColor + ((Progress) progress).getCount() + "<gray>/</gray><gold>" + ((Progress) progress).goal + "</gold><gray>)</gray>"));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString(getLabel()));
    }

    private String getLabel() {
        return display.isEmpty() ? "<gray>" + signal : display;
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        Progress signalProgress = (Progress) progress;
        return signalProgress.count >= signalProgress.goal;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getSignal() {
        return signal;
    }

    public IntRange getAmount() {
        return amount;
    }

    public String getDisplay() {
        return display;
    }

    @Override
    public Progress createNewProgress() {
        return new Progress(0);
    }

    @Override
    public void resolveGoal(ObjectiveProgress progress, long seed) {
        ((Progress) progress).goal = amount.roll(seed);
    }

    @Override
    public boolean onSignal(String signal, int amount, ObjectiveProgress progress) {
        if (!(progress instanceof Progress progress1) || !this.signal.equals(signal) || amount <= 0) return false;
        progress1.count = Math.min(progress1.goal, progress1.count + amount);
        return true;
    }
}
