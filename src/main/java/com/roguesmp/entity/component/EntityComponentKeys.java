package com.roguesmp.entity.component;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.component.impl.AttributeComponent;
import com.roguesmp.entity.component.impl.BehaviorComponent;
import com.roguesmp.entity.component.impl.BossBarComponent;
import com.roguesmp.entity.component.impl.DisplayNameComponent;
import com.roguesmp.entity.component.impl.EquipmentComponent;
import com.roguesmp.entity.component.impl.NameplateComponent;
import com.roguesmp.entity.component.impl.PhaseComponent;
import com.roguesmp.entity.component.impl.SpellComponent;
import com.roguesmp.registry.Registries;

public class EntityComponentKeys {

    public static final EntityComponentKey<DisplayNameComponent> DISPLAY_NAME;
    public static final EntityComponentKey<BehaviorComponent> BEHAVIOR;
    public static final EntityComponentKey<AttributeComponent> ATTRIBUTES;
    public static final EntityComponentKey<EquipmentComponent> EQUIPMENT;
    public static final EntityComponentKey<SpellComponent> SPELLS;
    public static final EntityComponentKey<BossBarComponent> BOSS_BAR;
    public static final EntityComponentKey<NameplateComponent> NAMEPLATE;
    public static final EntityComponentKey<PhaseComponent> PHASE;

    static {
        DISPLAY_NAME = register("display_name", DisplayNameComponent.CODEC);
        BEHAVIOR = register("behavior", BehaviorComponent.CODEC);
        ATTRIBUTES = register("attributes", AttributeComponent.CODEC);
        EQUIPMENT = register("equipment", EquipmentComponent.CODEC);
        SPELLS = register("spells", SpellComponent.CODEC);
        BOSS_BAR = register("boss_bar", BossBarComponent.CODEC);
        NAMEPLATE = register("nameplate", NameplateComponent.CODEC);
        PHASE = new EntityComponentKey<>("phase"); // Code-driven only, no CODEC (BossHealthAction isn't JSON-serializable).
    }

    public static void loadClass() {

    }

    public static <T extends EntityComponent> EntityComponentKey<T> register(String id, Codec<T> codec) {
        EntityComponentKey<T> key = new EntityComponentKey<>(id);
        Registries.ENTITY_COMPONENT_CODEC.register(id, codec);
        return key;
    }
}
