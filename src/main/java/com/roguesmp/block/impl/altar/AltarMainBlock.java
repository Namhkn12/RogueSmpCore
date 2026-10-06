package com.roguesmp.block.impl.altar;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.gui.AltarFusionGui;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.FusionRecipe;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AltarMainBlock extends AltarBlock {

    private @Nullable FusionRitual ritual;
    private @Nullable AltarNameDisplay nameDisplay;

    public AltarMainBlock(BlockProperties properties) {
        super(properties);
    }

    public FusionAssessment assess() {
        if (ritual != null) return FusionAssessment.failed(FusionIssue.BUSY);

        ItemStack input = getItem();
        if (input == null) return FusionAssessment.failed(FusionIssue.NO_INPUT);

        AltarSideSlot[] slots = AltarSideSlot.values();
        List<AltarSideBlock> sides = new ArrayList<>(slots.length);
        ItemStack[] items = new ItemStack[slots.length + 1];
        items[0] = input.clone();
        for (int i = 0; i < slots.length; i++) {
            AltarSideBlock side = getSide(slots[i]);
            if (side == null) return FusionAssessment.failed(FusionIssue.STRUCTURE_INCOMPLETE);

            sides.add(side);
            ItemStack sideItem = side.getItem();
            items[i + 1] = sideItem == null ? null : sideItem.clone();
        }

        FusionRecipe recipe = CraftingManager.getInstance().match(CraftingRecipes.FUSION, items, 0, 0);
        if (recipe == null || recipe.getResultStack() == null) return FusionAssessment.failed(FusionIssue.NO_RECIPE);
        return new FusionAssessment(FusionIssue.NONE, recipe, sides, items);
    }

    public FusionIssue startFusion(Player player) {
        FusionAssessment assessment = assess();
        if (assessment.isReady()) ritual = new FusionRitual(this, assessment, player.getUniqueId());
        return assessment.issue();
    }

    @Override
    public void setItem(@Nullable ItemStack newItem) {
        clearName();
        super.setItem(newItem);
    }

    void showResultName(ItemStack result) {
        clearName();
        nameDisplay = new AltarNameDisplay(getHoverLocation(), result.effectiveName());
    }

    @Override
    public void tick() {
        if (ritual == null) {
            super.tick();
            if (nameDisplay != null && nameDisplay.tick()) nameDisplay = null;
        } else if (ritual.tick()) {
            ritual = null;
        }
    }

    private void clearName() {
        if (nameDisplay == null) return;

        nameDisplay.remove();
        nameDisplay = null;
    }

    @Override
    protected void onEmptyHandClick(Player player) {
        if (player.isSneaking()) takeItem(player);
        else new AltarFusionGui(this).showInventory(player);
    }

    @Override
    public void onUnload() {
        if (ritual != null) {
            ritual.cancel();
            ritual = null;
        }
        clearName();
        super.onUnload();
    }

    public @Nullable AltarSideBlock getSide(AltarSideSlot slot) {
        SmpBlock block = BlockManager.getInstance().get(AltarLayout.positionOf(getPos(), slot));
        return block instanceof AltarSideBlock side && side.isHydrated() ? side : null;
    }
}
