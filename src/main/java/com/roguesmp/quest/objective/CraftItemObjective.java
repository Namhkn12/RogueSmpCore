package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.event.SmpCraftEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.goal.IntRange;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CraftItemObjective implements QuestObjective {

    public static final String TYPE_KEY = "craft_item";

    public static final class Progress implements ObjectiveProgress {
        public static final Codec<Progress> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("crafted", 0).forGetter(Progress::getCrafted),
                Progress::new
        );

        private int crafted;
        private int goal;

        public Progress(int crafted) {
            this.crafted = crafted;
        }

        public int getCrafted() {
            return crafted;
        }

        public int getGoal() {
            return goal;
        }

        @Override
        public boolean isDefault() {
            return crafted == 0;
        }

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public static final Codec<CraftItemObjective> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(CraftItemObjective::getItem),
            IntRange.CODEC.optionalFieldOf("amount", IntRange.exactly(1)).forGetter(CraftItemObjective::getAmount),
            CraftItemObjective::new
    );

    private final String item;
    private final IntRange amount;

    public CraftItemObjective(String item, IntRange amount) {
        this.item = item;
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        Progress craftProgress = (Progress) progress;
        String countColor = isCompleted(progress) ? "<gold>" : "<yellow>";
        return List.of(Utils.fromString("<gray>Chế tạo </gray>").append(ItemRef.displayName(item))
                .append(Utils.fromString(" <gray>(</gray>" + countColor + craftProgress.crafted + "<gray>/</gray><gold>" + craftProgress.goal + "</gold><gray>)</gray>")));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString("<gray>Chế tạo " + amount.display() + " </gray>").append(ItemRef.displayName(item)));
    }

    @Override
    public float completion(ObjectiveProgress progress) {
        Progress counted = (Progress) progress;
        return counted.goal <= 0 ? 1f : Math.min(1f, (float) counted.crafted / counted.goal);
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        Progress craftProgress = (Progress) progress;
        return craftProgress.crafted >= craftProgress.goal;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getItem() {
        return item;
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
    public boolean onCraft(SmpCraftEvent event, ObjectiveProgress progress) {
        if (!(progress instanceof Progress craftProgress) || !item.equals(ItemRef.idOf(event.getResult()))) return false;
        craftProgress.crafted = Math.min(craftProgress.goal, craftProgress.crafted + event.getTotalAmount());
        return true;
    }
}
