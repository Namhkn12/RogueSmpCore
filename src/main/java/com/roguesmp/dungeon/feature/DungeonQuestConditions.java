package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.quest.condition.QuestCondition;
import com.roguesmp.quest.condition.QuestConditions;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.quest.holder.QuestHolder;
import org.bukkit.entity.Player;

import java.util.Optional;

public class DungeonQuestConditions {

    // A dungeon's own quest group only counts inside that same instance.
    public record InDungeon(Optional<String> dungeon) implements QuestCondition {
        public static final Codec<InDungeon> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("dungeon").forGetter(InDungeon::dungeon),
                InDungeon::new
        );

        @Override
        public String getTypeId() {
            return "in_dungeon";
        }

        @Override
        public boolean test(QuestHolder holder, Player player) {
            DungeonInstance instance = DungeonManager.getInstance().getByWorld(player.getWorld());
            if (instance == null || !instance.isMember(player.getUniqueId())) return false;
            if (dungeon.isPresent() && !dungeon.get().equals(instance.dungeonId())) return false;
            if (holder instanceof GroupHolder group && group.owner().equals(QuestsFeature.OWNER)) return group.id().equals(instance.id());
            return true;
        }
    }

    public static final Codec<InDungeon> IN_DUNGEON = QuestConditions.register("in_dungeon", InDungeon.CODEC);

    public static void loadClass() {

    }
}
