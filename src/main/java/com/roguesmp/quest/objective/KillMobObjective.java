package com.roguesmp.quest.objective;

import com.google.gson.JsonObject;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.ObjectiveProgress;
import com.roguesmp.quest.QuestObjective;
import com.roguesmp.registry.entity.EntityRegistry;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public class KillMobObjective implements QuestObjective {

    public static final class Progress implements ObjectiveProgress {
        private int killed;

        public Progress(int killed) {
            this.killed = killed;
        }

        public int getKilled() {
            return killed;
        }

        public void setKilled(int killed) {
            this.killed = killed;
        }

        @Override
        public boolean isDefault() {
            return killed == 0;
        }
    }

    private final String mobId;

    private final int amount;

    public KillMobObjective(String mobId, int amount) {
        this.mobId = mobId;
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer, ObjectiveProgress progress) {
        BaseEntity base = EntityRegistry.getInstance().getBaseEntity(mobId);
        String mobName = (base != null) ? base.getDisplayName() : mobId;

        String countColor = isCompleted(progress) ? "<gold>" : "<yellow>";

        String displayString = String.format(
                "<gray>Tiêu diệt </gray><green>%s</green> <gray>(</gray>%s%d<gray>/</gray><gold>%d</gold><gray>)</gray>",
                mobName, countColor, ((Progress) progress).getKilled(), amount
        );

        return List.of(Utils.fromString(displayString));
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {

        BaseEntity base = EntityRegistry.getInstance().getBaseEntity(mobId);
        String mobName = (base != null) ? base.getDisplayName() : mobId;

        String displayString = String.format(
                "<gray>Tiêu diệt %s </gray><green>%s</green>",
                amount, mobName
        );
        return List.of(Utils.fromString(displayString));
    }

    @Override
    public boolean isCompleted(ObjectiveProgress progress) {
        return ((Progress) progress).killed >= amount;
    }

    @Override
    public JsonObject serializeProgress(ObjectiveProgress progress) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("killed", ((Progress) progress).killed);
        return jsonObject;
    }

    @Override
    public Progress deserializeProgress(JsonObject data) {
        int killed = data.get("killed") == null ? 0 : data.get("killed").getAsInt();
        return new Progress(killed);
    }

    @Override
    public Progress createNewProgress() {
        return new Progress(0);
    }

    @Override
    public void onKillEntity(EntityDeathEvent event, ObjectiveProgress progress) {
        if (!(progress instanceof Progress progress1)) {
            return;
        }
        Entity entity = event.getEntity();
        BaseEntity base = EntityUtils.getBaseEntity(entity);
        if (base == null) return;
        if (base.getId().equals(mobId)) progress1.killed = progress1.killed + 1;
    }
}
