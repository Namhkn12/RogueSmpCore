package com.roguesmp.constant;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.impl.*;
import com.roguesmp.registry.Registries;

public class ComponentKeys {

    public static final ComponentKey<NameComponent> ITEM_NAME;
    public static final ComponentKey<StackSizeComponent> STACK_SIZE;
    public static final ComponentKey<EnchantComponent> ENCHANT;
    public static final ComponentKey<EquipAttributeComponent> ATTRIBUTE;
    public static final ComponentKey<DurabilityComponent> DURABILITY;
    public static final ComponentKey<DescriptionComponent> DESCRIPTION;
    public static final ComponentKey<GemSocketComponent> GEM_SOCKET;
    public static final ComponentKey<GemDataComponent> GEM_DATA;
    public static final ComponentKey<ConsumableComponent> CONSUMABLE;
    public static final ComponentKey<PotionContentComponent> POTION_CONTENT;
    public static final ComponentKey<PlayerHeadSkinComponent> HEAD_SKIN;
    public static final ComponentKey<ItemModelComponent> ITEM_MODEL;
    public static final ComponentKey<BrokenComponent> BROKEN;
    public static final ComponentKey<DurabilityRepairComponent> DURABILITY_REPAIR;

    static {
        ITEM_NAME = register("name", NameComponent.CODEC);
        STACK_SIZE = register("stack_size", StackSizeComponent.CODEC);
        ENCHANT = register("enchant", EnchantComponent.CODEC);
        ATTRIBUTE = register("attribute", EquipAttributeComponent.CODEC);
        DURABILITY = register("durability", DurabilityComponent.CODEC);
        DESCRIPTION = register("description", DescriptionComponent.CODEC);
        GEM_SOCKET = register("socket", GemSocketComponent.CODEC);
        GEM_DATA = register("gem_data", GemDataComponent.CODEC);
        CONSUMABLE = register("consumable", ConsumableComponent.CODEC);
        POTION_CONTENT = register("potion_content", PotionContentComponent.CODEC);
        HEAD_SKIN = register("head_skin", PlayerHeadSkinComponent.CODEC);
        ITEM_MODEL = register("item_model", ItemModelComponent.CODEC);
        BROKEN = new ComponentKey<>("broken"); //Transient so it has no CODEC.
        DURABILITY_REPAIR = register("durability_repair", DurabilityRepairComponent.CODEC);
    }

    public static void loadClass() {

    }

    public static <T extends ItemComponent> ComponentKey<T> register(String id, Codec<T> codec) {
        ComponentKey<T> key = new ComponentKey<>(id);
        Registries.ITEM_COMPONENT_CODEC.register(id, codec);
        return key;
    }
}
