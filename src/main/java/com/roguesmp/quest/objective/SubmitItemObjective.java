package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.GateDisplay;
import com.roguesmp.gate.GateInventoryUtils;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.goal.IntRange;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Completed by handing in the full amount of an item at once; nothing is taken until the player carries all of it.
 */
public class SubmitItemObjective implements QuestObjective {

    public static final String TYPE_KEY = "submit_item";

    public static final class Progress implements ObjectiveProgress {
        public static final Codec<Progress> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("submitted", 0).forGetter(Progress::getSubmitted),
                Progress::new
        );

        private int submitted;
        private int goal;

        public Progress(int submitted) {
            this.submitted = submitted;
        }

        public int getSubmitted() {
            return submitted;
        }

        public int getGoal() {
            return goal;
        }

        @Override
        public boolean isDefault() {
            return submitted == 0;
        }

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public static final Codec<SubmitItemObjective> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(SubmitItemObjective::getItem),
            IntRange.CODEC.optionalFieldOf("amount", IntRange.exactly(1)).forGetter(SubmitItemObjective::getAmount),
            SubmitItemObjective::new
    );

    private final String item;
    private final IntRange amount;

    public SubmitItemObjective(String item, IntRange amount) {
        this.item = item;
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        Progress submitProgress = (Progress) progress;
        boolean completed = isCompleted(progress);
        int owned = completed || smpPlayer == null ? submitProgress.submitted : Math.min(submitProgress.goal, GateInventoryUtils.count(smpPlayer, item));
        String countColor = completed ? "<gold>" : owned >= submitProgress.goal ? "<green>" : "<red>";
        return List.of(Utils.fromString("<gray>Nộp </gray>").append(GateDisplay.itemName(item))
                .append(Utils.fromString(" <gray>(</gray>" + countColor + owned + "<gray>/</gray><gold>" + submitProgress.goal + "</gold><gray>)</gray>")));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString("<gray>Nộp " + amount.display() + " </gray>").append(GateDisplay.itemName(item)));
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        Progress submitProgress = (Progress) progress;
        return submitProgress.submitted >= submitProgress.goal;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
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
    public boolean onSubmitItems(SmpPlayer smpPlayer, ObjectiveProgress progress) {
        Player player = smpPlayer.getBukkitPlayer();
        if (!(progress instanceof Progress submitProgress) || player == null) return false;
        if (GateInventoryUtils.count(smpPlayer, item) < submitProgress.goal) return false;
        GateInventoryUtils.remove(player.getInventory(), item, submitProgress.goal);
        submitProgress.submitted = submitProgress.goal;
        return true;
    }

    public String getItem() {
        return item;
    }

    public IntRange getAmount() {
        return amount;
    }
}
