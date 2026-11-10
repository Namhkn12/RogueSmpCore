package com.roguesmp.worldboss.definition;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.destination.WorldSpawnDestination;
import com.roguesmp.utils.WorldPos;
import org.bukkit.Material;

import java.util.List;

public record WorldBossDefinition(String id, String displayName, List<String> description, Material icon,
                                  WorldPos location, String arena, int minParticipants, List<Requirement> requirements,
                                  SpawnMode spawn, int fightTimeoutSeconds, String lootTable,
                                  TeleportDestination exit) {

    public static final TeleportDestination DEFAULT_EXIT = new WorldSpawnDestination(null);

    public static final Codec<WorldBossDefinition> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(WorldBossDefinition::id),
            Codec.STRING.fieldOf("display_name").forGetter(WorldBossDefinition::displayName),
            Codec.listOf(Codec.STRING).optionalFieldOf("description", List.of()).forGetter(WorldBossDefinition::description),
            Codec.MATERIAL.optionalFieldOf("icon", Material.NETHER_STAR).forGetter(WorldBossDefinition::icon),
            Codec.WORLD_POS.fieldOf("location").forGetter(WorldBossDefinition::location),
            Codec.STRING.fieldOf("arena").forGetter(WorldBossDefinition::arena),
            Codec.INT.optionalFieldOf("min_participants", 1).forGetter(WorldBossDefinition::minParticipants),
            Codec.listOf(Requirement.CODEC).optionalFieldOf("requirements", List.of()).forGetter(WorldBossDefinition::requirements),
            SpawnMode.CODEC.fieldOf("spawn").forGetter(WorldBossDefinition::spawn),
            Codec.INT.fieldOf("fight_timeout_seconds").forGetter(WorldBossDefinition::fightTimeoutSeconds),
            Codec.STRING.fieldOf("loot_table").forGetter(WorldBossDefinition::lootTable),
            TeleportDestination.CODEC.optionalFieldOf("exit", DEFAULT_EXIT).forGetter(WorldBossDefinition::exit),
            WorldBossDefinition::new
    );

    public boolean meetsRequirements(SmpPlayer player) {
        return requirements.stream().allMatch(requirement -> requirement.test(player));
    }
}
