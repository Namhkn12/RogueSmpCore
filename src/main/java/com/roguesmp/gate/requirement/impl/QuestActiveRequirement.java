package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class QuestActiveRequirement implements Requirement {

    public static final String TYPE_KEY = "quest_active";
    public static final Codec<QuestActiveRequirement> CODEC = Registry.referenceCodec(() -> Registries.QUEST).fieldOf("quest")
            .xmap(QuestActiveRequirement::new, QuestActiveRequirement::getQuest).codec();

    private final Holder<Quest> quest;

    public QuestActiveRequirement(Holder<Quest> quest) {
        this.quest = quest;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public Holder<Quest> getQuest() {
        return quest;
    }

    @Override
    public boolean test(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !quest.isBound()) return false;
        return QuestManager.getInstance().getActiveQuests(bukkitPlayer).stream()
                .anyMatch(progress -> progress.getQuest() == quest.value());
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        String name = quest.isBound() && quest.value().getName() != null ? quest.value().getName() : quest.getId();
        return Utils.fromString("<gray>Đang làm nhiệm vụ <white>" + name);
    }
}
