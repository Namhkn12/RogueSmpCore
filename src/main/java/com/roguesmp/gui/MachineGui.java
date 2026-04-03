package com.roguesmp.gui;

import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.interfaces.IHaveLockedRecipe;
import com.roguesmp.constant.Keys;
import com.roguesmp.gui.interfaces.IHaveBlueprint;
import com.roguesmp.gui.interfaces.IHaveInputOutput;
import com.roguesmp.item.BaseItem;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public abstract class MachineGui extends BaseGui implements IHaveInputOutput, IHaveBlueprint {

    private final ItemStack INPUT_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.BLUE_STAINED_GLASS_PANE));
    private final ItemStack OUTPUT_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.ORANGE_STAINED_GLASS_PANE));
    private final ItemStack PROCESSING_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.GRAY_STAINED_GLASS_PANE));
    private final ItemStack SETTING_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.GREEN_STAINED_GLASS_PANE));
    private final ItemStack RECIPE_BROWSER = ItemStack.of(Material.ENCHANTED_BOOK);
    private final ItemStack SETTINGS = ItemStack.of(Material.NETHER_STAR);
    private final ItemStack ENERGY_VIEW = ItemStack.of(Material.REDSTONE_BLOCK);
    private final ItemStack BLUEPRINT_NULL = ItemStack.of(Material.RED_STAINED_GLASS_PANE);

    private final int SETTING_SLOT = 8;
    private final int RECIPE_SLOT = 7;
    private final int ENERGY_SLOT = 4;
    private final int BLUEPRINT_SLOT = 13;

    protected SmpMachine machine;
    private final int row;
    private final String name;

    /**
     * Instantiate the machine GUI
     * @param name: The name of the machine to show in the left
     * @param row: The number of rows that the GUI have except the setting row,
     *           the first row is automatically the settings row,
     *           the rest belongs to the machine itself
     */
    public MachineGui(SmpMachine machine, String name, int row) {
        super(Utils.fromString(name), row+1);
        this.row = row + 1;
        this.name = name;
        this.machine = machine;

        setup();
    }

    @Override
    public void setup() {
        fillMachineSides();
        itemDecoration();
        for(int i: getInputSlots()){
            if(i > 9*row)
                throw new IllegalArgumentException("Input slots for this " + name + " machine is outside gui");
            else{
                this.addButton(i, null, event -> {});
            }
        }
        for(int i: getOutputSlots()){
            if(i > 9*row)
                throw new IllegalArgumentException("Output slots for this " + name + " machine is outside gui");
            else{
                this.addButton(i, null, event -> {});
            }
        }

        this.addButton(SETTING_SLOT, SETTINGS, ClickHandler.openGui(new MachineTransferGui(machine)));
        this.addButton(RECIPE_SLOT, RECIPE_BROWSER, event -> {
            event.setCancelled(true);

            String machineId = machine.getItem().getId();
            List<BaseRecipe> recipes = RecipeManager.getMachineRecipesForMachine(machineId);

            RecipeBrowserGui browserGui = new RecipeBrowserGui("Công thức chế tạo", recipes, machine);
            event.getWhoClicked().openInventory(browserGui.getInventory());
        });
        setupBlueprintSlot();
    }

    private void fillMachineSides(){
        for(int i=0; i<9; i++){
            this.addButton(i, SETTING_FILLER, ClickHandler.noAction());
        }
        for(int i=1; i < row; i++){
            int thisRowProcessSlot = i * 9 + 4;
            for(int j=0; j<9; j++){
                int thisSlot = i * 9 + j;
                if(thisSlot < thisRowProcessSlot) this.addButton(thisSlot, INPUT_FILLER, ClickHandler.noAction());
                else if(thisSlot > thisRowProcessSlot) this.addButton(thisSlot, OUTPUT_FILLER, ClickHandler.noAction());
                else this.addButton(thisSlot, PROCESSING_FILLER, ClickHandler.noAction());
            }
        }
    }

    private void itemDecoration(){
        ItemStackUtils.setItemName(SETTINGS, Component.text("Cài đặt", NamedTextColor.GREEN));
        ItemStackUtils.setItemName(RECIPE_BROWSER, Component.text("Xem công thức", NamedTextColor.GREEN));
        ItemStackUtils.setItemName(ENERGY_VIEW, Component.text("Năng lượng hiện tại", NamedTextColor.GREEN));
        ItemStackUtils.setItemName(BLUEPRINT_NULL, Component.text("Khoá công thức", NamedTextColor.GREEN));
        ItemStackUtils.setLore(BLUEPRINT_NULL, List.of(
                Component.empty(),
                Component.text("Chuột phải với thành phẩm chính để khóa công thức").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));
    }

    @Override
    public void setupBlueprintSlot(){
        if(machine instanceof IHaveLockedRecipe mBlueprint){
            BaseRecipe lockedRecipe = mBlueprint.getLockedRecipe();
            ItemStack display;

            if(lockedRecipe != null){
                display = lockedRecipe.getOutputs().get(0).clone();
                addLoreToBlueprint(display);
            }
            else{
                display = BLUEPRINT_NULL.clone();
            }

            this.addButton(BLUEPRINT_SLOT, display, event -> {
                event.setCancelled(true);

                HumanEntity whoClicked = event.getWhoClicked();

                if(event.isLeftClick()){
                    BaseRecipe recipe = mBlueprint.getLockedRecipe();
                    if(recipe != null){
                        mBlueprint.setLockedRecipe(null);

                        this.getInventory().setItem(BLUEPRINT_SLOT, BLUEPRINT_NULL);
                        whoClicked.sendMessage(Component.text("Bỏ khóa công thức thành công").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                    }
                }
                if(event.isRightClick()){
                    ItemStack cursorItem = event.getCursor();
                    String itemId = cursorItem.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
                    BaseRecipe recipeFound = RecipeManager.findRecipeBasedOnMainOutput(itemId, machine.getItem().getId());

                    if(recipeFound != null){
                        ItemStack displayItem = cursorItem.clone();

                        addLoreToBlueprint(displayItem);
                        displayItem.setAmount(1);

                        this.getInventory().setItem(BLUEPRINT_SLOT, displayItem);
                        mBlueprint.setLockedRecipe(recipeFound);

                        whoClicked.sendMessage(Component.text("Khoá công thức thành công").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                    }
                    else{
                        whoClicked.sendMessage(Component.text("Máy không có công thức cho item này!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                    }
                }
            });
        }
        else{
            throw new NotImplementedException();
        }
    }

    @Override
    public void setBlueprintItem(BaseRecipe lockedRecipe) {
        ItemStack display;

        if(lockedRecipe != null){
            display = lockedRecipe.getOutputs().get(0).clone();
            addLoreToBlueprint(display);
        }
        else{
            display = BLUEPRINT_NULL.clone();
        }

        this.getInventory().setItem(BLUEPRINT_SLOT, display);
    }

    private void addLoreToBlueprint(ItemStack display) {
        List<Component> displayLore = new ArrayList<>(display.getData(DataComponentTypes.LORE).styledLines());
        displayLore.add(Component.empty());
        displayLore.add(Component.text("Chuột phải với thành phẩm chính để khóa công thức").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        displayLore.add(Component.text("Chuột trái để bỏ khóa công thức").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

        ItemStackUtils.setLore(display, displayLore);
    }

    /**
     * Input slots for the machine
     * Need to minus 9 for each slot
     */
    public abstract int[] getInputSlots();
    /**
     * Output slots for the machine
     * Need to minus 9 for each slot
     */
    public abstract int[] getOutputSlots();
    /**
     * The processing itemStack inside the machine
     * Need to minus 9
     */
    public abstract int getProcessingSlot();

    public void setProcessing(ItemStack processing){
        this.addButton(getProcessingSlot(), processing, ClickHandler.noAction());
    }

    public void setProcessingDefault(){
        this.addButton(getProcessingSlot(), PROCESSING_FILLER, ClickHandler.noAction());
    }

    public void setEnergy(int energy, int maxEnergy){
        Component e = Component.text("e", NamedTextColor.YELLOW);

        // 1. Clone item để không bị ghi đè lên item gốc của class
        ItemStack displayIcon = ENERGY_VIEW.clone();

        // 2. Sửa lore trên item đã clone
        ItemStackUtils.setLore(displayIcon,
                List.of(Component.text(energy + " / " + maxEnergy).color(NamedTextColor.WHITE).append(e).decoration(TextDecoration.ITALIC, false))
        );

        // 3. Cập nhật vào ButtonMap (để lần sau mở lại vẫn thấy)
        this.addButton(ENERGY_SLOT, displayIcon, ClickHandler.noAction());

        // 4. ÉP CẬP NHẬT TRỰC TIẾP LÊN GUI ĐANG MỞ (Quan trọng nhất)
        if (this.getInventory() != null) {
            this.getInventory().setItem(ENERGY_SLOT, displayIcon);
        }
    }
}
