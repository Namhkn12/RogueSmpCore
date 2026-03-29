package com.roguesmp.gui;

import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.EnergyRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Pair;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RecipeBrowserGui extends BaseGui {

    private final List<? extends BaseRecipe> recipes;
    private final SmpMachine machine;
    private int currentPage = 0;

    private final ItemStack BACK_BTN = ItemStack.of(Material.ARROW);
    private final ItemStack NEXT_PAGE = ItemStack.of(Material.SPECTRAL_ARROW);
    private final ItemStack PREV_PAGE = ItemStack.of(Material.SPECTRAL_ARROW);
    private final ItemStack FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));

    private final int ITEMS_PER_PAGE = 45;

    public RecipeBrowserGui(String title, List<? extends BaseRecipe> recipes, @Nullable SmpMachine machine){
        super(Utils.fromString(title), 6);
        this.recipes = recipes;
        this.machine = machine;

        itemDecoration();
        setup();
    }

    private void itemDecoration(){
        ItemStackUtils.setItemName(BACK_BTN, Component.text("Quay lại", NamedTextColor.GREEN));
        ItemStackUtils.setItemName(NEXT_PAGE, Component.text("Trang sau", NamedTextColor.GREEN));
        ItemStackUtils.setItemName(PREV_PAGE, Component.text("Trang trước", NamedTextColor.GREEN));
    }

    @Override
    public void setup() {
        getInventory().clear();

        for (int i = 45; i < 54; i++) {
            this.addButton(i, FILLER, ClickHandler.noAction());
        }

        if (machine != null) {
            this.addButton(49, BACK_BTN, event -> {
                event.setCancelled(true);// Render lại GUI máy móc
                event.getWhoClicked().openInventory(machine.getGui().getInventory());
            });
        }

        int maxPage = (int) Math.ceil((double) recipes.size() / ITEMS_PER_PAGE) - 1;
        if (maxPage < 0) maxPage = 0;

        if (currentPage > 0) {
            this.addButton(45, PREV_PAGE, event -> {
                event.setCancelled(true);
                currentPage--;
                setup(); // Vẽ lại GUI với trang mới
            });
        }

        if (currentPage < maxPage) {
            this.addButton(53, NEXT_PAGE, event -> {
                event.setCancelled(true);
                currentPage++;
                setup();
            });
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, recipes.size());
        int slot = 0;

        for (int i = startIndex; i < endIndex; i++) {
            BaseRecipe recipe = recipes.get(i);

            Pair<ItemStack, Boolean> displayItem = createRecipeDisplayItem(recipe);

            this.addButton(slot, displayItem.getFirst(), displayItem.getSecond() ? event -> {
                ItemStack icon = new ItemStack(Material.BARRIER);

                if (!recipe.getOutputs().isEmpty()) {
                    icon = recipe.getOutputs().get(0).clone();
                }

                RecipeViewerGui gui = new RecipeViewerGui(this, machine.getGui(), recipe, icon);

                gui.showInventory(event.getWhoClicked());
            } : ClickHandler.noAction());
            slot++;
        }
    }

    private Pair<ItemStack, Boolean> createRecipeDisplayItem(BaseRecipe recipe) {
        //Lore
        List<Component> lore = new ArrayList<>();

        //Icon
        ItemStack icon = new ItemStack(Material.BARRIER);

        //Have click action
        boolean haveClickAction = false;

        if(recipe instanceof MachineRecipe machineRecipe){

            haveClickAction = true;

            if (!recipe.getOutputs().isEmpty()) {
                icon = recipe.getOutputs().get(0).clone();
            } else {
                icon = new ItemStack(Material.BARRIER); // Fallback nếu recipe lỗi
            }

            lore.add(Component.text("Click để xem công thức", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        }
        if(recipe instanceof EnergyRecipe energyRecipe){

            haveClickAction = false;

            if (!recipe.getInputs().isEmpty()) {
                icon = recipe.getInputs().get(0).clone();
            } else {
                icon = new ItemStack(Material.BARRIER); // Fallback nếu recipe lỗi
            }

            lore.add(Component.text("Thời gian đốt: ", NamedTextColor.WHITE)
                    .append(Component.text(energyRecipe.getBaseProcessTime() + "s", NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
        }

        ItemStackUtils.setLore(icon, lore);

        return new Pair<>(icon, haveClickAction);
    }
}
