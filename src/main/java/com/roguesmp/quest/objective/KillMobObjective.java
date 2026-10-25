package com.roguesmp.quest.objective;

import com.google.gson.JsonObject;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.goal.IntRange;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public class KillMobObjective implements QuestObjective {

    public static final String TYPE_KEY = "kill_mob";

    public static final class Progress implements ObjectiveProgress {
        public static final Codec<Progress> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("killed", 0).forGetter(Progress::getKilled),
                Progress::new
        );

        private int killed;
        private int goal;

        public Progress(int killed) {
            this.killed = killed;
        }

        public int getKilled() {
            return killed;
        }

        public void setKilled(int killed) {
            this.killed = killed;
        }

        public int getGoal() {
            return goal;
        }

        @Override
        public boolean isDefault() {
            return killed == 0;
        }

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public static final Codec<KillMobObjective> CODEC = Codec.composite(
            Codec.STRING.fieldOf("mobId").forGetter(KillMobObjective::getMobId),
            IntRange.CODEC.fieldOf("amount").forGetter(KillMobObjective::getAmount),
            KillMobObjective::new
    );

    private final String mobId;
    private final IntRange amount;

    public KillMobObjective(String mobId, IntRange amount) {
        this.mobId = mobId;
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress) {
        BaseEntity base = EntityManager.getInstance().getBaseEntity(mobId);
        String mobName = (base != null) ? base.getDisplayName() : mobId;

        String countColor = isCompleted(progress) ? "<gold>" : "<yellow>";

        String displayString = String.format(
                "<gray>Tiêu diệt </gray><green>%s</green> <gray>(</gray>%s%d<gray>/</gray><gold>%d</gold><gray>)</gray>",
                mobName, countColor, ((Progress) progress).getKilled(), ((Progress) progress).goal
        );

        return List.of(Utils.fromString(displayString));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {

        BaseEntity base = EntityManager.getInstance().getBaseEntity(mobId);
        String mobName = (base != null) ? base.getDisplayName() : mobId;

        String displayString = String.format(
                "<gray>Tiêu diệt %s </gray><green>%s</green>",
                amount.display(), mobName
        );
        return List.of(Utils.fromString(displayString));
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        Progress mobProgress = (Progress) progress;
        return mobProgress.killed >= mobProgress.goal;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public IntRange getAmount() {
        return amount;
    }

    public String getMobId() {
        return mobId;
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
    public boolean onKillEntity(EntityDeathEvent event, ObjectiveProgress progress) {
        if (!(progress instanceof Progress progress1)) {
            return false;
        }
        Entity entity = event.getEntity();
        BaseEntity base = EntityUtils.getBaseEntity(entity);
        if (base == null || !base.getId().equals(mobId)) return false;
        progress1.killed = progress1.killed + 1;
        return true;
    }
}
