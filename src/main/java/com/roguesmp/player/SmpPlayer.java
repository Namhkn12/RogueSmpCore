package com.roguesmp.player;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.*;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.gui.enchant.GrindstoneGui;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.ConsumableComponent;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
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

    private final Map<EquipSlot, SmpItem> currentEquipment = new EnumMap<>(EquipSlot.class);

    private final AbilityLoadout abilityLoadout;

    private final Map<UUID, PlayerProjectile> projectiles = new HashMap<>();
    private int projectileCleanupTimer = 0;

    public SmpPlayer(UUID uuid) {
        this.uuid = uuid;
        abilityLoadout = new AbilityLoadout(this);
        activeEnchants = new EnumMap<>(Enchants.class);
        activeAttributes = new EnumMap<>(Attributes.class);
    }

    public SmpPlayer(Player bukkitPlayer) {
        this(bukkitPlayer.getUniqueId());
    }

    public void loadData(PlayerData playerData) {
        abilityLoadout.loadData(playerData);
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

        if (newItem != null) {
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

    public void trackProjectile(Projectile projectile, Map<Enchants, Integer> projEnchant, Map<Attributes, Double> projAttribute) {
        projectiles.put(projectile.getUniqueId(), new PlayerProjectile(this, projectile, projEnchant, projAttribute));
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

        activeEnchants.forEach((enchants, integer) -> enchants.getEnchant().tick(this, periodIncrement, integer));
        activeAttributes.forEach((attributes, aDouble) -> attributes.getAttribute().tick(this, periodIncrement, aDouble));
        abilityLoadout.tick(periodIncrement);
    }

    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.GRINDSTONE) {
            new GrindstoneGui(this).showInventory(player);
            event.setCancelled(true);
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (action.isLeftClick()) {
            abilityLoadout.cast(AbilityTrigger.Key.LEFT_CLICK);
        } else if (action.isRightClick()) {
            if (event.isBlockInHand()) return;
            abilityLoadout.cast(AbilityTrigger.Key.RIGHT_CLICK);
        }
    }

    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        abilityLoadout.cast(AbilityTrigger.Key.SWAP);
        event.setCancelled(true);
    }

    public void onInput(PlayerInputEvent event) {
        if (event.getInput().isJump()) abilityLoadout.cast(AbilityTrigger.Key.JUMP);
        else if (event.getInput().isSneak()) {
            abilityLoadout.cast(AbilityTrigger.Key.SNEAK);
        }
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
            SmpItem smpItem = SmpItem.wrap(consumed, this);
            smpItem.applyModifiers(this);

            ConsumableComponent consumableComponent = smpItem.getComponent(ComponentKeys.CONSUMABLE);
            if (consumableComponent != null) {
                consumableComponent.applyEffects(event.getPlayer());
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

    public void onBlockPlace(BlockPlaceEvent event) {
        EquipSlot equipSlot = EquipSlot.fromVanilla(event.getHand().getGroup());
        SmpItem smpItem = currentEquipment.get(equipSlot);
        if (smpItem != null && smpItem.getBaseItem() != null && BlockRegistry.getBlock(smpItem.getBaseItem().getId()) == null) { //Prevent placing custom items
            event.setCancelled(true);
        }
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

    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        Player bukkitPlayer = getBukkitPlayer();
        if (bukkitPlayer == null) return;

        ItemStack offhand = bukkitPlayer.getEquipment().getItemInOffHand();
        // Disallow shooting stuff from offhand, because calculating stats with it is very buggy
        if (ItemStackUtils.isShootableItem(offhand)) {
            bukkitPlayer.sendMessage(Component.text("Bạn không thể ném/bắn khi có vũ khí ở tay phụ!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        event.getProjectile().setPersistent(false);
        this.trackProjectile(event.getProjectile(), activeEnchants, activeAttributes);
        PlayerProjectile playerProjectile = this.getProjectile(event.getProjectile().getUniqueId());
        if (playerProjectile == null) return;
        playerProjectile.onProjectileLaunch(event);
        abilityLoadout.onProjectileLaunch(event);

        SmpItem currentMainhand = currentEquipment.get(EquipSlot.MAINHAND);
        if (currentMainhand != null) {
            DurabilityComponent durabilityComponent = currentMainhand.getComponent(ComponentKeys.DURABILITY);
            if (durabilityComponent != null) {
                durabilityComponent.setCurrentDurability(durabilityComponent.currentDurability() - 1);
                bukkitPlayer.getEquipment().getItemInMainHand().editPersistentDataContainer(durabilityComponent::save);
            }
        }
    }

    public void onShootArrow(EntityShootBowEvent event) {
        Player bukkitPlayer = getBukkitPlayer();
        if (bukkitPlayer == null) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            bukkitPlayer.sendMessage(Component.text("Bạn không thể bắn tên từ tay phụ.", NamedTextColor.RED));
            event.setCancelled(true);
        }

        event.getProjectile().setPersistent(false);
        this.trackProjectile((Projectile) event.getProjectile(), activeEnchants, activeAttributes);
        PlayerProjectile playerProjectile = this.getProjectile(event.getProjectile().getUniqueId());
        if (playerProjectile == null) return;
        playerProjectile.onShootArrow(event);
        abilityLoadout.onShootArrow(event);
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
