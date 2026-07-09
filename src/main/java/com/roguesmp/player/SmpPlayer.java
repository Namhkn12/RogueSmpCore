package com.roguesmp.player;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.*;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.gui.enchant.GrindstoneGui;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.*;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import com.roguesmp.player.mechanic.*;
import com.roguesmp.registry.BlockRegistry;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class SmpPlayer {

    private final UUID uuid;
    private final Map<Enchants, Integer> activeEnchants;
    private final Map<Attributes, Double> activeAttributes;

    private final Map<Enchants, Integer> activeEnchantsView;
    private final Map<Attributes, Double> activeAttributesView;

    private final Map<EquipSlot, SmpItem> currentEquipment = new EnumMap<>(EquipSlot.class);

    private final AbilityLoadout abilityLoadout;

    private final List<PlayerMechanic> mechanics = new ArrayList<>();

    private final Map<UUID, PlayerProjectile> projectiles = new HashMap<>();
    private int projectileCleanupTimer = 0;

    public SmpPlayer(UUID uuid) {
        this.uuid = uuid;
        abilityLoadout = new AbilityLoadout(this);
        activeEnchants = new EnumMap<>(Enchants.class);
        activeAttributes = new EnumMap<>(Attributes.class);

        activeEnchantsView = Collections.unmodifiableMap(activeEnchants);
        activeAttributesView = Collections.unmodifiableMap(activeAttributes);

        initMechanic();
    }

    public SmpPlayer(Player bukkitPlayer) {
        this(bukkitPlayer.getUniqueId());
    }

    public void loadData(PlayerData playerData) {
        abilityLoadout.loadData(playerData);
    }

    private void initMechanic() {
        mechanics.add(new OffhandBlockerMechanic());
        mechanics.add(new CustomBlockPlacementMechanic());
        mechanics.add(new EnchantMechanic());
        mechanics.add(new AttributeMechanic());
        mechanics.add(new AbilityLoadoutMechanic());
        mechanics.add(new ProjectileMechanic());
        mechanics.add(new ItemConsumableMechanic());
        mechanics.add(new DurabilityLossMechanic());

        mechanics.sort(Comparator.comparingInt(PlayerMechanic::getPriority));
    }

    public void updateSlotStat(Player player, EquipSlot slot, @Nullable SmpItem newItem) {
        SmpItem oldItem = currentEquipment.get(slot);
        Set<Attributes> affected = new HashSet<>();

        if (oldItem != null) {
            EquipAttributeComponent oldComp = oldItem.getComponent(ComponentKeys.ATTRIBUTE);
            if (oldComp != null && oldComp.getSlot() == slot) {
                oldComp.getFinalAttributes().forEach((attr, val) -> {
                    affected.add(attr);
                    activeAttributes.merge(attr, -val, (oldV, delta) -> {
                        double res = oldV + delta;
                        return Utils.isEffectiveZero(res) ? null : res;
                    });
                });
            }
            processEnchantDelta(oldItem, slot, -1);
            currentEquipment.remove(slot);
        }

        if (newItem != null && !newItem.hasComponent(ComponentKeys.BROKEN)) {
            EquipAttributeComponent newComp = newItem.getComponent(ComponentKeys.ATTRIBUTE);
            if (newComp != null && newComp.getSlot() == slot) {
                newComp.getFinalAttributes().forEach((attr, val) -> {
                    affected.add(attr);
                    activeAttributes.merge(attr, val, (oldV, delta) -> {
                        double res = oldV + delta;
                        return Utils.isEffectiveZero(res) ? null : res;
                    });
                });
            }
            processEnchantDelta(newItem, slot, 1);
            currentEquipment.put(slot, newItem);
        }

        for (Attributes attr : affected) {
            Double total = activeAttributes.get(attr); // Null if pruned above
            if (total == null) {
                attr.getAttribute().removeVanillaAttribute(player);
            } else {
                attr.getAttribute().addVanillaAttribute(player, total);
            }
        }
    }

    private void processEnchantDelta(SmpItem item, EquipSlot slot, int multiplier) {
        EnchantComponent enchantComp = item.getComponent(ComponentKeys.ENCHANT);
        if (enchantComp == null) return;

        enchantComp.getTotalEnchants().forEach((ench, level) -> {
            // Only apply if the enchant is valid for the current equipment slot
            if (ench.getEnchant().getActiveSlots().contains(slot)) {
                activeEnchants.merge(ench, level * multiplier, (oldVal, delta) -> {
                    int result = oldVal + delta;
                    return result <= 0 ? null : result;
                });
            }
        });
    }

    public void registerMechanic(PlayerMechanic mechanic) {
        if (!mechanics.contains(mechanic)) {
            mechanics.add(mechanic);
            mechanics.sort(Comparator.comparingInt(PlayerMechanic::getPriority));
        }
    }

    public @Unmodifiable Map<Enchants, Integer> getActiveEnchants() {
        return activeEnchantsView;
    }

    public @Unmodifiable Map<Attributes, Double> getActiveAttributes() {
        return activeAttributesView;
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

    public @Nullable SmpItem getItemAtEquipSlot(EquipSlot equipSlot) {
        return currentEquipment.get(equipSlot);
    }

    public UUID getUuid() {
        return uuid;
    }

    public PlayerData getPlayerData() {
        return PlayerManager.getInstance().getDataManager().getData(uuid);
    }

    public @Nullable Player getBukkitPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void sendMessage(Component text) {
        Player bukkitPlayer = getBukkitPlayer();
        if (bukkitPlayer == null) return;
        bukkitPlayer.sendMessage(text);
    }

    public void sendMessage(String text) {
        Player bukkitPlayer = getBukkitPlayer();
        if (bukkitPlayer == null) return;
        bukkitPlayer.sendMessage(text);
    }

    public @Nullable PlayerProjectile getProjectile(UUID uuid) {
        return projectiles.get(uuid);
    }

    public PlayerProjectile trackProjectile(Projectile projectile, Map<Enchants, Integer> projEnchant, Map<Attributes, Double> projAttribute) {
        PlayerProjectile playerProjectile = new PlayerProjectile(this, projectile, projEnchant, projAttribute);
        projectiles.put(projectile.getUniqueId(), playerProjectile);
        return playerProjectile;
    }

    public @Nullable PlayerProjectile untrackProjectile(UUID uuid) {
        return projectiles.remove(uuid);
    }

    public void tick(int periodIncrement) {

        projectileCleanupTimer = projectileCleanupTimer + periodIncrement;
        if (projectileCleanupTimer > 100) {
            projectileCleanupTimer = 0;
            var projectileIterator = projectiles.entrySet().iterator();
            while (projectileIterator.hasNext()) {
                var playerProjectile = projectileIterator.next().getValue();
                if (playerProjectile.shouldRemove()) {
                    Projectile projectile = playerProjectile.getProjectile();
                    if (projectile != null) projectile.remove();
                    projectileIterator.remove();
                } else {
                    playerProjectile.incrementTickAlive(periodIncrement);
                }
            }
        }

        for (PlayerMechanic mechanic : mechanics) {
            mechanic.tick(periodIncrement, this);
        }
    }

    public void onInteract(PlayerInteractEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onInteract(event, this);
        }
    }

    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onSwapHand(event, this);
        }
    }

    public void onInput(PlayerInputEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onInput(event, this);
        }
    }

    public void onDamageEntity(DamageEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onDamageEntity(event, this);
        }

    }

    public void onKillEntity(EntityDeathEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onKillEntity(event, this);
        }
    }

    public void onHurt(DamageEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onHurt(event, this);
        }
    }

    public void onHurtFatal(DamageEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onHurtFatal(event, this);
        }
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onConsume(event, this);
        }
    }

    public void onExpChange(PlayerExpChangeEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onExpChange(event, this);
        }
    }

    public void onBlockBreak(BlockBreakEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onBlockBreak(event, this);
        }
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onBlockPlace(event, this);
        }
    }

    /**
     * Called when player is set on fire
     */
    public void onCombust(EntityCombustEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onCombust(event, this);
        }
    }

    /**
     * Called when player set other entities (not player) on fire (including from projectiles)
     */
    public void onCombustEntity(EntityCombustByEntityEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onCombustEntity(event, this);
        }
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onProjectileHit(event, this);
        }
    }

    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onProjectileLaunch(event, this);
        }
    }

    public void onShootArrow(EntityShootBowEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onShootArrow(event, this);
        }
    }

    public void onConsumeArrow(ArrowConsumeEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onConsumeArrow(event, this);
        }
    }
}
