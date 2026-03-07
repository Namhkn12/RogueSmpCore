package com.roguesmp.player;

import com.roguesmp.constant.*;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class SmpPlayer {

    private final UUID uuid;
    private final Map<Enchants, Integer> activeEnchants;
    private final Map<Attributes, Double> activeAttributes;

    private final AbilityLoadout abilityLoadout;

    private final Map<UUID, PlayerProjectile> projectiles = new HashMap<>();

    public SmpPlayer(UUID uuid) {
        this.uuid = uuid;
        abilityLoadout = new AbilityLoadout(this);
        activeEnchants = new EnumMap<>(Enchants.class);
        activeAttributes = new EnumMap<>(Attributes.class);
    }

    public SmpPlayer(Player player) {
        this(player.getUniqueId());
    }

    public void loadData() {
        PlayerData playerData = getPlayerData();
        abilityLoadout.loadData(playerData);
    }

    public void saveData() {
        PlayerData playerData = getPlayerData();
        abilityLoadout.saveData(playerData);
    }

    public void updateSlotStat(Player player, EquipSlot slot, @Nullable SmpItem oldItem, @Nullable SmpItem newItem) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().removeVanillaAttribute(player);
        });

        if (oldItem != null) {
            EnchantComponent oldEnchant = oldItem.getComponent(ComponentKeys.ENCHANT);
            EquipAttributeComponent oldAttribute = oldItem.getComponent(ComponentKeys.ATTRIBUTE);

            if (oldEnchant != null) {
                oldEnchant.getEnchants().forEach((enchants, integer) -> {
                    Set<EquipSlot> activeSlot = enchants.getEnchant().getActiveSlots();
                    if (activeSlot.contains(slot)) {
                        activeEnchants.merge(enchants, -integer, (integer1, integer2) -> {
                            int res = integer1 + integer2;
                            if (res == 0) return null;
                            return res;
                        });
                    }
                });
            }
            if (oldAttribute != null) {
                oldAttribute.getAttributes().forEach((attributes, aDouble) -> {
                    if (oldAttribute.getSlot() == slot) {
                        activeAttributes.merge(attributes, -aDouble, (aDouble1, aDouble2) -> {
                            double res = aDouble1 + aDouble2;
                            if (Utils.isEffectiveZero(res)) return null;
                            return res;
                        });
                    }

                });
            }

        }

        if (newItem != null) {
            EnchantComponent newEnchant = newItem.getComponent(ComponentKeys.ENCHANT);
            EquipAttributeComponent newAttribute = newItem.getComponent(ComponentKeys.ATTRIBUTE);

            if (newEnchant != null) {
                newEnchant.getEnchants().forEach((enchants, integer) -> {
                    Set<EquipSlot> activeSlot = enchants.getEnchant().getActiveSlots();
                    if (activeSlot.contains(slot)) {
                        activeEnchants.merge(enchants, integer, (integer1, integer2) -> {
                            int res = integer1 + integer2;
                            if (res == 0) return null;
                            return res;
                        });
                    }
                });
            }
            if (newAttribute != null) {
                newAttribute.getAttributes().forEach((attributes, aDouble) -> {
                    if (newAttribute.getSlot() == slot) {
                        activeAttributes.merge(attributes, aDouble, (aDouble1, aDouble2) -> {
                            double res = aDouble1 + aDouble2;
                            if (Utils.isEffectiveZero(res)) return null;
                            return res;
                        });
                    }
                });
            }
        }

        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().addVanillaAttribute(player, aDouble);
        });

    }

    public @Unmodifiable Map<Enchants, Integer> getActiveEnchants() {
        return Collections.unmodifiableMap(activeEnchants);
    }

    public @Unmodifiable Map<Attributes, Double> getActiveAttributes() {
        return Collections.unmodifiableMap(activeAttributes);
    }

    public AbilityLoadout getAbilityLoadout() {
        return abilityLoadout;
    }

    public UUID getUuid() {
        return uuid;
    }

    public PlayerData getPlayerData() {
        return PlayerDataManager.getInstance().getData(uuid);
    }

    public @Nullable Player getBukkitPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public @Nullable PlayerProjectile getProjectile(UUID uuid) {
        return projectiles.get(uuid);
    }

    public void trackProjectile(Projectile projectile) {
        projectiles.put(projectile.getUniqueId(), new PlayerProjectile(this, projectile, activeEnchants, activeAttributes));
    }

    public @Nullable PlayerProjectile untrackProjectile(UUID uuid) {
        return projectiles.remove(uuid);
    }

    public void tick(boolean twoHz, boolean oneHz) {
        abilityLoadout.tick(oneHz, twoHz);
        activeEnchants.forEach((enchants, integer) -> enchants.getEnchant().tick(this, integer, twoHz, oneHz));
        activeAttributes.forEach((attributes, aDouble) -> attributes.getAttribute().tick(this, aDouble, twoHz, oneHz));

    }

    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        if (action.isLeftClick()) {
            if (player.isSneaking()) abilityLoadout.cast(AbilityTrigger.SHIFT_LEFT_CLICK);
            else abilityLoadout.cast(AbilityTrigger.LEFT_CLICK);
        } else if (action.isRightClick()) {
            if (player.isSneaking()) abilityLoadout.cast(AbilityTrigger.SHIFT_RIGHT_CLICK);
            else abilityLoadout.cast(AbilityTrigger.RIGHT_CLICK);
        }
    }

    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer().isSneaking()) abilityLoadout.cast(AbilityTrigger.SHIFT_SWAP);
        else abilityLoadout.cast(AbilityTrigger.SWAP);
        event.setCancelled(true);
    }

    public void onDamageEntity(DamageEvent event) {
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            // Player damage an entity with projectile
            PlayerProjectile playerProjectile = this.getProjectile(projectile.getUniqueId());
            if (playerProjectile == null) return;
            abilityLoadout.onDamageEntity(event);
            playerProjectile.onDamageEntity(event);
            return;
        }

        if (event.getDamager() instanceof Player) {
            // Player damage (melee) an entity
            abilityLoadout.onDamageEntity(event);
            activeEnchants.forEach((enchants, integer) -> {
                enchants.getEnchant().onDamageEntity(event, integer, this);
            });
            activeAttributes.forEach((attributes, aDouble) -> {
                attributes.getAttribute().onDamageEntity(event, aDouble, this);
            });
        }

    }

    public void onKillEntity(EntityDeathEvent event) {
        abilityLoadout.onKillEntity(event);
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onKillEntity(event, integer, this);
        });
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onKillEntity(event, aDouble, this);
        });
    }

    public void onHurt(DamageEvent event) {
        getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onHurt(event, integer, this);
        });
        getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onHurt(event, aDouble, this);
        });
    }

    public void onHurtFatal(DamageEvent event) {
        abilityLoadout.onHurtFatal(event);
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onHurtFatal(event, integer, this);
        });
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onHurtFatal(event, aDouble, this);
        });
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        abilityLoadout.onConsume(event);
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onConsume(event, integer, this);
        });
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onConsume(event, aDouble, this);
        });
    }

    public void onExpChange(PlayerExpChangeEvent event) {
        abilityLoadout.onExpChange(event);
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onExpChange(event, integer, this);
        });
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onExpChange(event, aDouble, this);
        });
    }

    public void onBlockBreak(BlockBreakEvent event) {
        abilityLoadout.onBlockBreak(event);
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onBlockBreak(event, integer, this);
        });
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onBlockBreak(event, aDouble, this);
        });
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        PlayerProjectile playerProjectile = this.getProjectile(event.getEntity().getUniqueId());
        if (playerProjectile == null) return;
        abilityLoadout.onProjectileHit(event);
        playerProjectile.onProjectileHit(event);
        if (event.getHitBlock() != null) {
            Utils.runLater(() -> this.untrackProjectile(event.getEntity().getUniqueId()));
        }
    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Player player = getBukkitPlayer();
        if (player == null) return;
        if (player.isSneaking()) abilityLoadout.cast(AbilityTrigger.SHIFT_PROJECTILE);

        event.getEntity().setPersistent(false);
        this.trackProjectile(event.getEntity());
        PlayerProjectile playerProjectile = this.getProjectile(event.getEntity().getUniqueId());
        if (playerProjectile == null) return;
        abilityLoadout.onProjectileLaunch(event);
        playerProjectile.onProjectileLaunch(event);
        // Untrack in case the projectile never hit anything
        Utils.runLater(() -> this.untrackProjectile(event.getEntity().getUniqueId()), 20 * 10);
    }
}
