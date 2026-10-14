package com.roguesmp.quest.condition;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;

public class QuestConditions {

    public record InWorld(String world) implements QuestCondition {
        public static final Codec<InWorld> CODEC = Codec.composite(
                Codec.STRING.fieldOf("world").forGetter(InWorld::world),
                InWorld::new
        );

        @Override
        public String getTypeId() {
            return "world";
        }

        @Override
        public boolean test(QuestHolder holder, Player player) {
            return player.getWorld().getName().equals(world);
        }
    }

    public record InRegion(String region) implements QuestCondition {
        public static final Codec<InRegion> CODEC = Codec.composite(
                Codec.STRING.fieldOf("region").forGetter(InRegion::region),
                InRegion::new
        );

        @Override
        public String getTypeId() {
            return "region";
        }

        @Override
        public boolean test(QuestHolder holder, Player player) {
            return ServerRegionManager.getInstance().regionIdsAt(player.getLocation()).contains(region);
        }
    }

    public static final Codec<InWorld> WORLD = register("world", InWorld.CODEC);
    public static final Codec<InRegion> REGION = register("region", InRegion.CODEC);

    public static void loadClass() {

    }

    public static <T extends QuestCondition> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.QUEST_CONDITION_CODEC.register(typeName, codec);
    }
}
