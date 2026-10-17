package com.roguesmp.crafting.gui.category;

import com.roguesmp.block.BlockType;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class RecipeCategory<T extends CraftingRecipe> {

    protected static final ItemStack EMPTY_CELL = BaseGui.createDecoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> vanillaCatalysts;
    private final Set<BlockType<?, ?>> catalystBlocks;

    protected RecipeCategory(String id, String displayName, Material icon, List<String> vanillaCatalysts, Set<BlockType<?, ?>> catalystBlocks) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.vanillaCatalysts = List.copyOf(vanillaCatalysts);
        this.catalystBlocks = Set.copyOf(catalystBlocks);
    }

    public abstract void layout(T recipe, RecipeLayout layout);

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public List<String> vanillaCatalysts() {
        return vanillaCatalysts;
    }

    public Set<BlockType<?, ?>> catalystBlocks() {
        return catalystBlocks;
    }

    protected ItemStack info(String... lines) {
        ItemStack item = ItemStack.of(icon);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(displayName, NamedTextColor.AQUA));
        if (lines.length > 0) {
            List<Component> lore = new ArrayList<>(lines.length);
            for (String line : lines) lore.add(Utils.text(line, NamedTextColor.GRAY));
            item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }
        return item;
    }

    protected static ItemStack progress(Material icon, int ticks) {
        ItemStack item = ItemStack.of(icon);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Thời gian: " + Utils.formatDecimal(ticks / 20d) + "s", NamedTextColor.YELLOW));
        return item;
    }
}
