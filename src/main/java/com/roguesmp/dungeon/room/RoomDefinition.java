package com.roguesmp.dungeon.room;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.script.DungeonValidation;

import java.util.List;

public record RoomDefinition(RoomType type, String name, String schematic, List<RoomObjective> objectives, String boss, List<RoomEffect<?>> effects) {

    public static final Codec<RoomDefinition> CODEC = Codec.composite(
            Codec.enumOf(RoomType.class).optionalFieldOf("type", RoomType.NORMAL).forGetter(RoomDefinition::type),
            Codec.STRING.optionalFieldOf("name", "").forGetter(RoomDefinition::name),
            Codec.STRING.fieldOf("schematic").forGetter(RoomDefinition::schematic),
            Codec.listOf(RoomObjective.CODEC).optionalFieldOf("objectives", List.of()).forGetter(RoomDefinition::objectives),
            Codec.STRING.optionalFieldOf("boss", "").forGetter(RoomDefinition::boss),
            Codec.listOf(RoomEffect.CODEC).optionalFieldOf("effects", List.of()).forGetter(RoomDefinition::effects),
            RoomDefinition::new
    );

    public void validate(DungeonValidation check) {
        if (!RoomSchematics.names().contains(schematic)) check.warn("unknown schematic '" + schematic + "'");
        if (!boss.isEmpty()) check.checkMob(boss);
        for (int i = 0; i < objectives.size(); i++) {
            RoomObjective objective = objectives.get(i);
            DungeonValidation at = check.at("objectives[" + i + "]");
            at.at("on").listens(objective.on());
            for (int j = 0; j < objective.conditions().size(); j++) objective.conditions().get(j).validate(at.at("conditions[" + j + "]"));
        }
    }
}
