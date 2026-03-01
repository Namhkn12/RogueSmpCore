package com.roguesmp.constant;

import com.google.common.reflect.TypeToken;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.registry.ItemComponentCodecRegistry;
import com.roguesmp.item.component.impl.*;
import com.roguesmp.item.component.serialize.ComponentCodec;

import java.util.List;

public class ComponentKeys {

    public static final ComponentKey<NameComponent> ITEM_NAME;
    public static final ComponentKey<EnchantComponent> ENCHANT;
    public static final ComponentKey<EquipAttributeComponent> ATTRIBUTE;
    public static final ComponentKey<DurabilityComponent> DURABILITY;
    public static final ComponentKey<DescriptionComponent> DESCRIPTION;
    public static final ComponentKey<GemSocketComponent> GEM_SOCKET;
    public static final ComponentKey<GemDataComponent> GEM_DATA;

    static {
        ITEM_NAME = ItemComponentCodecRegistry.register("name",
                ComponentCodec.singleArg(String.class, NameComponent::new, NameComponent::value));
        ENCHANT = ItemComponentCodecRegistry.register("enchant", EnchantComponent.class);
        ATTRIBUTE = ItemComponentCodecRegistry.register("attribute", EquipAttributeComponent.class);
        DURABILITY = ItemComponentCodecRegistry.register("durability",
                ComponentCodec.singleArg(Integer.class, DurabilityComponent::new, DurabilityComponent::maxDurability));
        DESCRIPTION = ItemComponentCodecRegistry.register("description",
                ComponentCodec.singleArg(new TypeToken<List<String>>(){}.getType(), DescriptionComponent::new, DescriptionComponent::description));
        GEM_SOCKET = ItemComponentCodecRegistry.register("socket",
                ComponentCodec.singleArg(Integer.class, GemSocketComponent::new, GemSocketComponent::getSocketCount));
        GEM_DATA = ItemComponentCodecRegistry.register("gem_data", GemDataComponent.class);
    }

    public static void loadClass() {

    }
}
