package com.roguesmp.dungeon.feature;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.motion.SpinMotion;
import com.roguesmp.fx.render.ItemDisplayRenderer;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.joml.Vector3f;

import java.util.List;

public class ChestOpenAnimation {

    private static final int DURATION_TICKS = 45;
    private static final int ICON_CYCLE_TICKS = 5;
    private static final int BURST_TICK = 40;
    private static final float SPIN_DEGREES_PER_TICK = 8f;
    private static final float DISPLAY_SCALE = 0.7f;

    public void play(Player clicker, Block block, List<ItemStack> items, Runnable onComplete) {
        Location center = getAnimCenter(block);
        List<ItemStack> icons = items.isEmpty() ? List.of(new ItemStack(Material.CHEST)) : items;

        clicker.playSound(center, Sound.BLOCK_CHEST_OPEN, 1f, 0.7f);
        clicker.sendActionBar(Component.text("Opening...", NamedTextColor.GOLD));

        ItemDisplayRenderer renderer = new ItemDisplayRenderer(new Vector3f(DISPLAY_SCALE), 1, (display, point) -> {
            display.setItemStack(icons.get((point.tick() / ICON_CYCLE_TICKS) % icons.size()));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            display.setGlowing(true);
        });

        FxEngine.getInstance().play(FxEffect.builder(center.clone().add(0, 1.5, 0))
                .part(new FxPart(new PointShape(), renderer).motion(new SpinMotion(new Vector3f(0, 1, 0), SPIN_DEGREES_PER_TICK)))
                .duration(DURATION_TICKS)
                .viewer(clicker)
                .onComplete(() -> {
                    if (onComplete != null) onComplete.run();
                })
                .build());

        for (int tick = 0; tick < BURST_TICK; tick += 10) {
            Utils.runLater(() -> clicker.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.8, 0), 6, 0.3, 0.3, 0.3, 0.04), tick);
        }
        Utils.runLater(() -> {
            spawnFirework(clicker, center.clone().add(0, 1.0, 0));
            clicker.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
            clicker.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.3f);
        }, BURST_TICK);
    }

    public void playAlreadyClaimed(Player clicker, Block block) {
        Location lid = getAnimCenter(block).clone().add(0, 0.9, 0);
        clicker.playSound(lid, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        for (double y = 0; y <= 1.0; y += 0.2) {
            clicker.spawnParticle(Particle.SMOKE, lid.clone().add(0, y, 0), 3, 0.03, 0.02, 0.03, 0.01);
        }
    }

    private Location getAnimCenter(Block block) {
        if (block.getState() instanceof Chest chest
                && chest.getInventory() instanceof DoubleChestInventory inventory
                && inventory.getHolder() instanceof DoubleChest doubleChest) {
            return doubleChest.getLocation().add(0.5, 0, 0.5);
        }
        return block.getLocation().add(0.5, 0, 0.5);
    }

    private void spawnFirework(Player viewer, Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class, f -> f.setVisibleByDefault(false));
        viewer.showEntity(RogueSmpCore.getInstance(), firework);

        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.WHITE, Color.YELLOW)
                .withFade(Color.ORANGE)
                .flicker(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);

        Utils.runLater(() -> {
            if (!firework.isDead()) firework.detonate();
        }, 1);
    }
}
