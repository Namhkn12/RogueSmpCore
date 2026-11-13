package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.event.SmpFishEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.goal.IntRange;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FishObjective implements QuestObjective {

    public static final String TYPE_KEY = "fish";

    public static final class Progress implements ObjectiveProgress {
        public static final Codec<Progress> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("caught", 0).forGetter(Progress::getCaught),
                Progress::new
        );

        private int caught;
        private int goal;

        public Progress(int caught) {
            this.caught = caught;
        }

        public int getCaught() {
            return caught;
        }

        public int getGoal() {
            return goal;
        }

        @Override
        public boolean isDefault() {
            return caught == 0;
        }

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public static final Codec<FishObjective> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("item").forGetter(objective -> Optional.ofNullable(objective.item)),
            Codec.STRING.optionalFieldOf("tier").forGetter(objective -> Optional.ofNullable(objective.tier)),
            Codec.STRING.optionalFieldOf("hotspot").forGetter(objective -> Optional.ofNullable(objective.hotspot)),
            IntRange.CODEC.optionalFieldOf("amount", IntRange.exactly(1)).forGetter(FishObjective::getAmount),
            (item, tier, hotspot, amount) -> new FishObjective(item.orElse(null), tier.orElse(null), hotspot.orElse(null), amount)
    );

    private final @Nullable String item;
    private final @Nullable String tier;
    private final @Nullable String hotspot;
    private final IntRange amount;

    public FishObjective(@Nullable String item, @Nullable String tier, @Nullable String hotspot, IntRange amount) {
        this.item = item;
        this.tier = tier;
        this.hotspot = hotspot;
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        Progress fishProgress = (Progress) progress;
        String countColor = isCompleted(progress) ? "<gold>" : "<yellow>";
        return List.of(describe(Utils.fromString("<gray>Câu </gray>"))
                .append(Utils.fromString(" <gray>(</gray>" + countColor + fishProgress.caught + "<gray>/</gray><gold>" + fishProgress.goal + "</gold><gray>)</gray>")));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(describe(Utils.fromString("<gray>Câu " + amount.display() + " </gray>")));
    }

    private Component describe(Component prefix) {
        Component target = item != null ? ItemRef.displayName(item) : Utils.fromString("<green>cá</green>");
        Component display = prefix.append(target);
        if (tier != null) display = display.append(Utils.fromString(" <gray>hạng </gray><green>" + tier + "</green>"));
        return display;
    }

    @Override
    public float completion(ObjectiveProgress progress) {
        Progress counted = (Progress) progress;
        return counted.goal <= 0 ? 1f : Math.min(1f, (float) counted.caught / counted.goal);
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        Progress fishProgress = (Progress) progress;
        return fishProgress.caught >= fishProgress.goal;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
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
    public boolean onFish(SmpFishEvent event, ObjectiveProgress progress) {
        if (!(progress instanceof Progress fishProgress)) return false;
        if (tier != null && !tier.equalsIgnoreCase(event.getTier().id())) return false;
        if (hotspot != null && !hotspot.equals(event.getHotspotId())) return false;

        int gained;
        if (item == null) {
            gained = 1;
        } else {
            int sum = 0;
            for (ItemStack stack : event.getCaught()) {
                if (item.equals(ItemRef.idOf(stack))) {
                    int itemStackAmount = stack.getAmount();
                    sum += itemStackAmount;
                }
            }
            gained = sum;
        }
        if (gained <= 0) return false;

        fishProgress.caught = Math.min(fishProgress.goal, fishProgress.caught + gained);
        return true;
    }
}
