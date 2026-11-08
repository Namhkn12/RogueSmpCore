package com.roguesmp.block.impl.crusher;

import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.CrusherRecipe;
import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.data.HandCrusherData;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class HandCrusherBlock extends SmpBlock {

    private static final int BAR_LENGTH = 10;
    private static final float ITEM_SCALE = 0.5f;
    private static final String VERB = "Nghiền";
    private static final String REJECT_MESSAGE = "Vật phẩm này không thể nghiền";
    private static final String DONE_MESSAGE = "Đã nghiền xong";
    private static final Sound WORK_SOUND = Sound.BLOCK_GRINDSTONE_USE;
    private static final Sound FINISH_SOUND = Sound.BLOCK_STONE_BREAK;

    private final HandCrusherData data;
    private @Nullable StoredItem held;
    private int progress;
    private @Nullable ItemDisplay heldDisplay;

    public HandCrusherBlock(BlockProperties properties, HandCrusherData data) {
        super(properties);
        this.data = data;
    }

    @Override
    public void onHydrated() {
        Utils.runLater(this::syncHeldDisplay);
    }

    @Override
    public boolean interactsWhileSneaking() {
        return true;
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.isSneaking()) takeBack(player);
        else if (held == null) insert(player, event.getItem());
        else work(player);
    }

    private void insert(Player player, @Nullable ItemStack hand) {
        StoredItem offered = StoredItem.of(hand);
        CrusherRecipe recipe = offered == null ? null : recipeFor(offered);
        if (hand == null || offered == null || recipe == null) {
            player.sendActionBar(Utils.text(REJECT_MESSAGE, NamedTextColor.RED));
            return;
        }

        int count = recipe.getInput().count();
        held = offered.withAmount(count);
        progress = 0;
        hand.setAmount(hand.getAmount() - count);
        markDirty();
        syncHeldDisplay();
        player.playSound(getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1f, 1f);
        showProgress(player, recipe);
    }

    private void work(Player player) {
        CrusherRecipe recipe = held == null ? null : recipeFor(held);
        if (recipe == null) {
            takeBack(player);
            return;
        }

        progress += Math.max(1, data.progressPerClick());
        markDirty();
        Location center = getLocation().toCenterLocation().add(0, 0.6, 0);
        ItemStack particleItem = held.toItemStack();
        if (particleItem != null) center.getWorld().spawnParticle(Particle.ITEM, center, 6, 0.15, 0.1, 0.15, 0.05, particleItem);
        center.getWorld().playSound(center, WORK_SOUND, 0.6f, 0.8f + Utils.RANDOM.nextFloat() * 0.4f);

        if (progress < recipe.getDuration()) {
            showProgress(player, recipe);
            return;
        }

        for (StoredItem result : recipe.roll()) drop(result);
        held = null;
        progress = 0;
        markDirty();
        syncHeldDisplay();
        center.getWorld().playSound(center, FINISH_SOUND, 1f, 0.9f);
        player.sendActionBar(Utils.text(DONE_MESSAGE, NamedTextColor.GREEN));
    }

    private void takeBack(Player player) {
        if (held == null) return;

        ItemStack stack = held.toItemStack();
        held = null;
        progress = 0;
        markDirty();
        syncHeldDisplay();
        if (stack != null) PlayerUtils.giveItem(player, stack);
        player.playSound(getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.8f);
    }

    private void syncHeldDisplay() {
        ItemStack stack = held == null ? null : held.withAmount(1).toItemStack();
        if (stack == null) {
            removeHeldDisplay();
            return;
        }
        if (heldDisplay != null && heldDisplay.isValid()) {
            heldDisplay.setItemStack(stack);
            return;
        }

        Location spot = getLocation().toBlockLocation().add(0.5, data.itemHeight(), 0.5);
        heldDisplay = spot.getWorld().spawn(spot, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setItemStack(stack);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f((float) (Math.PI / 2), 1, 0, 0),
                    new Vector3f(ITEM_SCALE),
                    new AxisAngle4f()
            ));
        });
    }

    private void removeHeldDisplay() {
        if (heldDisplay != null && heldDisplay.isValid()) heldDisplay.remove();
        heldDisplay = null;
    }

    private void showProgress(Player player, CrusherRecipe recipe) {
        int percent = Math.min(100, progress * 100 / recipe.getDuration());
        int filled = percent * BAR_LENGTH / 100;
        Component bar = Component.text("■".repeat(filled), NamedTextColor.GREEN)
                .append(Component.text("■".repeat(BAR_LENGTH - filled), NamedTextColor.DARK_GRAY));
        player.sendActionBar(Utils.text(VERB + " ", NamedTextColor.YELLOW).append(held.displayName())
                .append(Component.text(" ", NamedTextColor.GRAY)).append(bar)
                .append(Component.text(" " + percent + "%", NamedTextColor.GRAY)));
    }

    private @Nullable CrusherRecipe recipeFor(StoredItem item) {
        ItemStack stack = item.toItemStack();
        if (stack == null) return null;

        return CraftingManager.getInstance().match(CraftingRecipes.CRUSHER, new ItemStack[]{stack}, recipe -> recipe.getMinTier() <= data.tier() && recipe.matches(stack));
    }

    private void drop(StoredItem stored) {
        int remaining = stored.amount();
        int maxStack = Math.max(1, stored.maxStackSize());
        Location top = getLocation().toCenterLocation().add(0, 0.6, 0);
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStack);
            ItemStack stack = stored.withAmount(amount).toItemStack();
            if (stack == null) return;
            top.getWorld().dropItemNaturally(top, stack);
            remaining -= amount;
        }
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (held != null) drop(held);
        held = null;
        progress = 0;
        removeHeldDisplay();
    }

    @Override
    public void onUnload() {
        heldDisplay = null;
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(StoredItem.CODEC.optionalFieldOf("held"), () -> Optional.ofNullable(held), value -> held = value.orElse(null)));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("progress", 0), () -> progress, value -> progress = value));
    }
}
