package com.roguesmp.block.impl.altar;

import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

final class FusionRitual {

    private static final int STEP_TICKS = 10;
    private static final int CHARGE_STEPS = 4;
    private static final int CONVERGE_STEPS = 4;
    private static final int CHARGE_TICKS = CHARGE_STEPS * STEP_TICKS;
    private static final int TOTAL_TICKS = (CHARGE_STEPS + CONVERGE_STEPS) * STEP_TICKS;
    private static final float SIDE_RISE_PER_STEP = 0.15f;
    private static final float MAX_SIDE_RISE = SIDE_RISE_PER_STEP * CHARGE_STEPS;
    private static final float MAIN_RISE = 0.4f;

    private final AltarMainBlock main;
    private final FusionAssessment plan;
    private final @Nullable UUID initiator;
    private final Location mainHover;
    private final Location scratch;
    private final Location[] sideHovers;
    private final Vector3f[] sideOffsets;
    private int elapsed;

    FusionRitual(AltarMainBlock main, FusionAssessment plan, @Nullable UUID initiator) {
        this.main = main;
        this.plan = plan;
        this.initiator = initiator;
        this.mainHover = main.getHoverLocation();
        this.scratch = mainHover.clone();

        List<AltarSideBlock> sides = plan.sides();
        this.sideHovers = new Location[sides.size()];
        this.sideOffsets = new Vector3f[sides.size()];
        for (int i = 0; i < sides.size(); i++) {
            sideHovers[i] = sides.get(i).getHoverLocation();
            sideOffsets[i] = new Vector3f((float) (mainHover.getX() - sideHovers[i].getX()), 0, (float) (mainHover.getZ() - sideHovers[i].getZ()));
        }

        setLocked(true);
        playSound(Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
    }

    boolean tick() {
        if (!isStructureIntact()) {
            interrupt();
            return true;
        }

        if (elapsed % STEP_TICKS == 0) animateStep(elapsed / STEP_TICKS);
        spawnParticles();

        elapsed++;
        if (elapsed < TOTAL_TICKS) return false;

        complete();
        return true;
    }

    void cancel() {
        setLocked(false);
    }

    private boolean isStructureIntact() {
        for (AltarSideBlock side : plan.sides()) {
            if (BlockManager.getInstance().get(side.getPos()) != side) return false;
        }
        return true;
    }

    private void interrupt() {
        setLocked(false);
        mainHover.getWorld().spawnParticle(Particle.SMOKE, mainHover, 30, 0.4, 0.4, 0.4, 0.02);
        playSound(Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);

        Player player = initiator == null ? null : Bukkit.getPlayer(initiator);
        if (player != null) player.sendMessage(Utils.text("Nghi thức bị gián đoạn vì altar bị phá.", NamedTextColor.RED));
    }

    private void complete() {
        FusionRecipe recipe = plan.recipe();
        ItemStack[] remaining = recipe.consume(plan.items(), 0, 0);
        List<AltarSideBlock> sides = plan.sides();
        for (int i = 0; i < sides.size(); i++) {
            sides.get(i).setItem(remaining[i + 1]);
        }
        ItemStack result = plan.result();
        main.setItem(result);
        setLocked(false);
        main.showResultName(result);

        mainHover.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, mainHover, 40, 0.4, 0.4, 0.4, 0.3);
        mainHover.getWorld().spawnParticle(Particle.END_ROD, mainHover, 60, 0.3, 0.3, 0.3, 0.15);
        mainHover.getWorld().spawnParticle(Particle.FIREWORK, mainHover, 30, 0.5, 0.5, 0.5, 0);
        playSound(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        playSound(Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 1f, 1.4f);
        playSound(Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.3f);
    }

    private void animateStep(int step) {
        playSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f + step * 0.1f);
        if (step < CHARGE_STEPS) animateCharge(step);
        else animateConverge(step);
    }

    private void animateCharge(int step) {
        float rise = SIDE_RISE_PER_STEP * (step + 1);
        for (AltarSideBlock side : plan.sides()) {
            side.animateDisplay(new Vector3f(0, rise, 0), AltarItemDisplay.SPIN_STEP * (step + 1), AltarItemDisplay.ITEM_SCALE, STEP_TICKS);
        }

        float progress = (step + 1) / (float) CHARGE_STEPS;
        main.animateDisplay(new Vector3f(0, MAIN_RISE * progress, 0), -AltarItemDisplay.SPIN_STEP * (step + 1),
                AltarItemDisplay.ITEM_SCALE * (1 + 0.4f * progress), STEP_TICKS);
    }

    private void animateConverge(int step) {
        int convergeStep = step - CHARGE_STEPS;
        float progress = (convergeStep + 1) / (float) CONVERGE_STEPS;
        float spin = AltarItemDisplay.SPIN_STEP * (step + 1);

        List<AltarSideBlock> sides = plan.sides();
        for (int i = 0; i < sides.size(); i++) {
            Vector3f translation = new Vector3f(sideOffsets[i]).mul(progress);
            translation.y = MAX_SIDE_RISE + (MAIN_RISE - MAX_SIDE_RISE) * progress;
            sides.get(i).animateDisplay(translation, spin, AltarItemDisplay.ITEM_SCALE * (1 - 0.8f * progress), STEP_TICKS);
        }

        main.animateDisplay(new Vector3f(0, MAIN_RISE, 0), -spin, AltarItemDisplay.ITEM_SCALE * (1.4f + 0.2f * progress), STEP_TICKS);
    }

    private void spawnParticles() {
        if (elapsed < CHARGE_TICKS) spawnChargeParticles();
        else spawnConvergeParticles();
    }

    private void spawnChargeParticles() {
        if (elapsed % 2 != 0) return;

        float rise = SIDE_RISE_PER_STEP * (elapsed / STEP_TICKS + 1);
        for (Location hover : sideHovers) {
            scratch.set(hover.getX(), hover.getY() + rise, hover.getZ());
            scratch.getWorld().spawnParticle(Particle.PORTAL, scratch, 3, 0.3, 0.3, 0.3, 0.5);
        }
    }

    private void spawnConvergeParticles() {
        float progress = Math.min(1f, (elapsed - CHARGE_TICKS) / (float) (TOTAL_TICKS - CHARGE_TICKS));
        float height = MAX_SIDE_RISE + (MAIN_RISE - MAX_SIDE_RISE) * progress;
        for (int i = 0; i < sideHovers.length; i++) {
            Location hover = sideHovers[i];
            Vector3f offset = sideOffsets[i];
            scratch.set(hover.getX() + offset.x * progress, hover.getY() + height, hover.getZ() + offset.z * progress);
            scratch.getWorld().spawnParticle(Particle.END_ROD, scratch, 1, 0.05, 0.05, 0.05, 0.0);
            scratch.getWorld().spawnParticle(Particle.ENCHANT, scratch, 2, 0.2, 0.2, 0.2, 0.2);
        }
    }

    private void setLocked(boolean locked) {
        main.setLocked(locked);
        plan.sides().forEach(side -> side.setLocked(locked));
    }

    private void playSound(Sound sound, float volume, float pitch) {
        mainHover.getWorld().playSound(mainHover, sound, volume, pitch);
    }
}
