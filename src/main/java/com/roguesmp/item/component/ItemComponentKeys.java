package com.roguesmp.item.component;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.impl.*;
import com.roguesmp.registry.Registries;

public class ItemComponentKeys {

    public static final ComponentKey<NameComponent> ITEM_NAME = register("name", NameComponent.CODEC);
    public static final ComponentKey<StackSizeComponent> STACK_SIZE = register("stack_size", StackSizeComponent.CODEC);
    public static final ComponentKey<EnchantComponent> ENCHANT = register("enchant", EnchantComponent.CODEC);
    public static final ComponentKey<EquipAttributeComponent> ATTRIBUTE = register("attribute", EquipAttributeComponent.CODEC);
    public static final ComponentKey<DurabilityComponent> DURABILITY = register("durability", DurabilityComponent.CODEC);
    public static final ComponentKey<DescriptionComponent> DESCRIPTION = register("description", DescriptionComponent.CODEC);
    public static final ComponentKey<GemSocketComponent> GEM_SOCKET = register("socket", GemSocketComponent.CODEC);
    public static final ComponentKey<GemDataComponent> GEM_DATA = register("gem_data", GemDataComponent.CODEC);
    public static final ComponentKey<ConsumableComponent> CONSUMABLE = register("consumable", ConsumableComponent.CODEC);
    public static final ComponentKey<PotionContentComponent> POTION_CONTENT = register("potion_content", PotionContentComponent.CODEC);
    public static final ComponentKey<PlayerHeadSkinComponent> HEAD_SKIN = register("head_skin", PlayerHeadSkinComponent.CODEC);
    public static final ComponentKey<ItemModelComponent> ITEM_MODEL = register("item_model", ItemModelComponent.CODEC);
    //Transient so it has no CODEC.
    public static final ComponentKey<BrokenComponent> BROKEN = new ComponentKey<>("broken");
    public static final ComponentKey<DurabilityRepairComponent> DURABILITY_REPAIR = register("durability_repair", DurabilityRepairComponent.CODEC);
    public static final ComponentKey<WrenchComponent> WRENCH = register("wrench", WrenchComponent.CODEC);
    public static final ComponentKey<PassiveAbilityComponent> PASSIVE_ABILITY = register("passive_ability", PassiveAbilityComponent.CODEC);
    public static final ComponentKey<EnchantGlintComponent> ENCHANT_GLINT = register("glint", EnchantGlintComponent.CODEC);
    public static final ComponentKey<MagicPowerComponent> MAGIC_POWER = register("magic_power", MagicPowerComponent.CODEC);
    public static final ComponentKey<RandomStatComponent> RANDOM_STAT = register("random_stat", RandomStatComponent.CODEC);
    public static final ComponentKey<UsageTimerComponent> USAGE_TIMER = register("usage_timer", UsageTimerComponent.CODEC);
    public static final ComponentKey<CommandExecutorComponent> COMMAND_EXECUTOR = register("command_executor", CommandExecutorComponent.CODEC);

    public static void loadClass() {

    }

    public static <T extends ItemComponent> ComponentKey<T> register(String id, Codec<T> codec) {
        ComponentKey<T> key = new ComponentKey<>(id);
        Registries.ITEM_COMPONENT_CODEC.register(id, codec);
        return key;
    }
}
