package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.event.DungeonClearEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.goal.IntRange;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DungeonClearObjective implements QuestObjective {

    public static final String TYPE_KEY = "dungeon_clear";

    public static final class Progress implements ObjectiveProgress {
        public static final Codec<Progress> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("cleared", 0).forGetter(Progress::getCleared),
                Progress::new
        );

        private int cleared;
        private int goal;

        public Progress(int cleared) {
            this.cleared = cleared;
        }

        public int getCleared() {
            return cleared;
        }

        @Override
        public boolean isDefault() {
            return cleared == 0;
        }

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public static final Codec<DungeonClearObjective> CODEC = Codec.composite(
            Codec.STRING.fieldOf("dungeon").forGetter(DungeonClearObjective::getDungeon),
            IntRange.CODEC.optionalFieldOf("amount", IntRange.exactly(1)).forGetter(DungeonClearObjective::getAmount),
            Codec.STRING.optionalFieldOf("display", "").forGetter(DungeonClearObjective::getDisplayName),
            DungeonClearObjective::new
    );

    private final String dungeon;
    private final IntRange amount;
    private final String displayName;

    public DungeonClearObjective(String dungeon, IntRange amount, String displayName) {
        this.dungeon = dungeon;
        this.amount = amount;
        this.displayName = displayName;
    }

    private String label() {
        return "<gray>Hoàn thành phó bản </gray><white>" + (displayName.isEmpty() ? dungeon : displayName) + "</white>";
    }

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        Progress counted = (Progress) progress;
        String countColor = isCompleted(progress) ? "<gold>" : "<yellow>";
        return List.of(Utils.fromString(label() + " <gray>(</gray>" + countColor + counted.cleared + "<gray>/</gray><gold>" + counted.goal + "</gold><gray>)</gray>"));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString(label()));
    }

    @Override
    public float completion(ObjectiveProgress progress) {
        Progress counted = (Progress) progress;
        return counted.goal <= 0 ? 1f : Math.min(1f, (float) counted.cleared / counted.goal);
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        Progress counted = (Progress) progress;
        return counted.cleared >= counted.goal;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getDungeon() {
        return dungeon;
    }

    public IntRange getAmount() {
        return amount;
    }

    public String getDisplayName() {
        return displayName;
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
    public boolean onDungeonClear(DungeonClearEvent event, ObjectiveProgress progress) {
        if (!(progress instanceof Progress counted) || !dungeon.equals(event.getDungeonId())) return false;
        counted.cleared = Math.min(counted.goal, counted.cleared + 1);
        return true;
    }
}
