package com.roguesmp.player;

import com.roguesmp.constant.*;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.ConsumableComponent;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class SmpPlayer {

    private final UUID uuid;
    private final Map<Enchants, Integer> activeEnchants;
    private final Map<Attributes, Double> activeAttributes;

    private final Map<EquipSlot, SmpItem> slotCache = new EnumMap<>(EquipSlot.class);

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

    public void loadData(PlayerData playerData) {
        abilityLoadout.loadData(playerData);
    }

    public void updateSlotStat(Player player, EquipSlot slot, @Nullable SmpItem newItem) {

        SmpItem oldItem = slotCache.remove(slot);

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
                            if (res <= 0) return null; //If enchant is not positive then remove it
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
                            if (res <= 0) return null;
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

            slotCache.put(slot, newItem);
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

    public Map<Enchants, Integer> getActiveEnchantsCopy() {
        Map<Enchants, Integer> map = new EnumMap<>(Enchants.class);
        map.putAll(activeEnchants);
        return map;
    }

    public Map<Attributes, Double> getActiveAttributesCopy() {
        Map<Attributes, Double> map = new EnumMap<>(Attributes.class);
        map.putAll(activeAttributes);
        return map;
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

    public void sendMessage(Component text) {
        Player player = getBukkitPlayer();
        if (player == null) return;
        player.sendMessage(text);
    }

    public void sendMessage(String text) {
        Player player = getBukkitPlayer();
        if (player == null) return;
        player.sendMessage(text);
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

    public void tick(int periodIncrement) {
        activeEnchants.forEach((enchants, integer) -> enchants.getEnchant().tick(this, periodIncrement, integer));
        activeAttributes.forEach((attributes, aDouble) -> attributes.getAttribute().tick(this, periodIncrement, aDouble));
        abilityLoadout.tick(periodIncrement);
    }

    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (action.isLeftClick()) {
            if (player.isSneaking()) abilityLoadout.cast(AbilityTrigger.SHIFT_LEFT_CLICK);
        } else if (action.isRightClick()) {
            // Is it a valid casting tool? (Not food, not a bow, etc.)
            if (!ItemStackUtils.canBeCastedWith(event.getItem())) return;
            // Does it have the required stats to be considered a 'weapon'?
            if (!activeAttributes.containsKey(Attributes.MELEE_DAMAGE_BASE)) return;
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
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            // Player damage an entity with projectile
            PlayerProjectile playerProjectile = this.getProjectile(projectile.getUniqueId());
            if (playerProjectile == null) return;
            playerProjectile.onDamageEntity(event);
            abilityLoadout.onDamageEntity(event);
            return;
        }

        if (event.getDamager() instanceof Player) {
            // Player damage (melee) an entity
            activeAttributes.forEach((attributes, aDouble) -> {
                attributes.getAttribute().onDamageEntity(event, aDouble, this);
            });
            activeEnchants.forEach((enchants, integer) -> {
                enchants.getEnchant().onDamageEntity(event, integer, this);
            });
            abilityLoadout.onDamageEntity(event);
        }

    }

    public void onKillEntity(EntityDeathEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onKillEntity(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onKillEntity(event, integer, this);
        });
        abilityLoadout.onKillEntity(event);
    }

    public void onHurt(DamageEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onHurt(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onHurt(event, integer, this);
        });
        abilityLoadout.onHurt(event);
    }

    public void onHurtFatal(DamageEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onHurtFatal(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onHurtFatal(event, integer, this);
        });
        abilityLoadout.onHurtFatal(event);
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onConsume(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onConsume(event, integer, this);
        });
        abilityLoadout.onConsume(event);

        if (!event.isCancelled()) { //Handle Consumable component
            ItemStack consumed = event.getItem();
            BaseItem baseItem = SmpItemUtils.getBaseItem(consumed);
            if (baseItem != null) {
                SmpItem smpItem = new SmpItem(consumed);
                smpItem.applyModifiers(this);

                ConsumableComponent consumableComponent = smpItem.getComponent(ComponentKeys.CONSUMABLE);
                if (consumableComponent != null) {
                    consumableComponent.applyEffects(event.getPlayer());
                }
            }
        }
    }

    public void onExpChange(PlayerExpChangeEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onExpChange(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onExpChange(event, integer, this);
        });
        abilityLoadout.onExpChange(event);
    }

    public void onBlockBreak(BlockBreakEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onBlockBreak(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onBlockBreak(event, integer, this);
        });
        abilityLoadout.onBlockBreak(event);
    }

    /**
     * Called when player is set on fire
     */
    public void onCombust(EntityCombustEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onCombust(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onCombust(event, integer, this);
        });
        abilityLoadout.onCombust(event);
    }

    /**
     * Called when player set other entities (not player) on fire (including from projectiles)
     */
    public void onCombustEntity(EntityCombustByEntityEvent event) {
        Entity combuster = event.getCombuster();
        if (combuster instanceof Player) {
            activeAttributes.forEach((attributes, aDouble) -> {
                attributes.getAttribute().onCombustEntity(event, aDouble, this);
            });
            activeEnchants.forEach((enchants, integer) -> {
                enchants.getEnchant().onCombustEntity(event, integer, this);
            });
            abilityLoadout.onCombustEntity(event);
            return;
        }

        if (combuster instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            PlayerProjectile playerProjectile = this.getProjectile(event.getEntity().getUniqueId());
            if (playerProjectile == null) return;
            playerProjectile.onCombustEntity(event);
            abilityLoadout.onCombustEntity(event);

        }
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        PlayerProjectile playerProjectile = this.getProjectile(event.getEntity().getUniqueId());
        if (playerProjectile == null) return;
        playerProjectile.onProjectileHit(event);
        abilityLoadout.onProjectileHit(event);
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
        playerProjectile.onProjectileLaunch(event);
        abilityLoadout.onProjectileLaunch(event);
        // Untrack in case the projectile never hit anything
        Utils.runLater(() -> this.untrackProjectile(event.getEntity().getUniqueId()), 20 * 10);
    }

    public void onConsumeArrow(ArrowConsumeEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onConsumeArrow(event, aDouble, this);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onConsumeArrow(event, integer, this);
        });
        abilityLoadout.onConsumeArrow(event);
    }
}
