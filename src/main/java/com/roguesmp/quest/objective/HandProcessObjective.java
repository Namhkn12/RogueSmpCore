package com.roguesmp.quest.objective;

import com.roguesmp.block.BlockData;
import com.roguesmp.codec.Codec;
import com.roguesmp.event.HandMachineProcessEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.goal.IntRange;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Counts finished cycles on a hand-operated machine such as the hand crusher or hand sieve.
 */
public class HandProcessObjective implements QuestObjective {

    public static final String TYPE_KEY = "hand_process";

    public static final class Progress implements ObjectiveProgress {
        public static final Codec<Progress> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("cycles", 0).forGetter(Progress::getCycles),
                Progress::new
        );

        private int cycles;
        private int goal;

        public Progress(int cycles) {
            this.cycles = cycles;
        }

        public int getCycles() {
            return cycles;
        }

        public int getGoal() {
            return goal;
        }

        @Override
        public boolean isDefault() {
            return cycles == 0;
        }

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public static final Codec<HandProcessObjective> CODEC = Codec.composite(
            Codec.STRING.fieldOf("block").forGetter(HandProcessObjective::getBlock),
            IntRange.CODEC.optionalFieldOf("amount", IntRange.exactly(1)).forGetter(HandProcessObjective::getAmount),
            HandProcessObjective::new
    );

    private final String block;
    private final IntRange amount;

    public HandProcessObjective(String block, IntRange amount) {
        this.block = block;
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        Progress processProgress = (Progress) progress;
        String countColor = isCompleted(progress) ? "<gold>" : "<yellow>";
        return List.of(Utils.fromString("<gray>Vận hành </gray>" + blockName()
                + " <gray>(</gray>" + countColor + processProgress.cycles + "<gray>/</gray><gold>" + processProgress.goal + "</gold><gray>)</gray>"));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString("<gray>Vận hành </gray>" + blockName() + " <gray>" + amount.display() + " lần</gray>"));
    }

    private String blockName() {
        BlockData data = Registries.BLOCK.get(block);
        return data == null || data.properties().displayName().isEmpty() ? "<green>" + block + "</green>" : data.properties().displayName();
    }

    @Override
    public float completion(ObjectiveProgress progress) {
        Progress counted = (Progress) progress;
        return counted.goal <= 0 ? 1f : Math.min(1f, (float) counted.cycles / counted.goal);
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        Progress processProgress = (Progress) progress;
        return processProgress.cycles >= processProgress.goal;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getBlock() {
        return block;
    }

    public IntRange getAmount() {
        return amount;
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
    public boolean onHandMachineProcess(HandMachineProcessEvent event, ObjectiveProgress progress) {
        if (!(progress instanceof Progress processProgress) || !block.equals(event.getBlockId())) return false;
        processProgress.cycles = Math.min(processProgress.goal, processProgress.cycles + 1);
        return true;
    }
}
