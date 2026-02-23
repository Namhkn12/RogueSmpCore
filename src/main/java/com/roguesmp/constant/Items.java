package com.roguesmp.constant;

import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import org.bukkit.Material;

import java.util.Map;

public class Items {
    public static BaseItem STEEL_INGOT = new BaseItem("steel_ingot", Material.IRON_INGOT, Map.of("name", new NameComponent("Steel Ingot")));
    public static BaseItem STEEL_BLOCK = new BaseItem("steel_block", Material.IRON_BLOCK, Map.of("name", new NameComponent("Steel Block")));
}
