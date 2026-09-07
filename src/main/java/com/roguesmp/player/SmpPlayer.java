package com.roguesmp.player;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.*;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.player.ability.AbilityType;
import com.roguesmp.player.classes.PlayerClass;
import com.roguesmp.player.mechanic.*;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class SmpPlayer {

    private final UUID uuid;
    private final PlayerData playerData;
    private final Map<Enchants, Integer> activeEnchants;
    private final Map<Attributes, Double> activeAttributes;

    private final Map<Enchants, Integer> activeEnchantsView;
    private final Map<Attributes, Double> activeAttributesView;

    private final Map<EquipSlot, SmpItem> currentEquipment = new EnumMap<>(EquipSlot.class);
    private final Map<EquipSlot, SmpItem> currentEquipmentView = Collections.unmodifiableMap(currentEquipment);

    private final AbilityLoadout abilityLoadout;

    private final List<PlayerMechanic> mechanics = new ArrayList<>();

    private final Map<UUID, PlayerProjectile> projectiles = new HashMap<>();
    private int projectileCleanupTimer = 0;
    private @Nullable PlayerClass playerClass;

    // Wall-clock (not tick-count) cooldown tracking for passive item abilities (ItemAbility) -
    // lives here rather than on the ability/component instance since non-unique SmpItems (and
    // thus their components/abilities) get rebuilt fresh on every wrap and would otherwise lose
    // any cooldown state constantly.
    private final Map<String, Long> abilityCooldowns = new HashMap<>();

    public SmpPlayer(PlayerData playerData) {
        this.uuid = playerData.getUuid();
        this.playerData = playerData;
        abilityLoadout = new AbilityLoadout(this);
        activeEnchants = new EnumMap<>(Enchants.class);
        activeAttributes = new EnumMap<>(Attributes.class);

        activeEnchantsView = Collections.unmodifiableMap(activeEnchants);
        activeAttributesView = Collections.unmodifiableMap(activeAttributes);

        initMechanic();

        abilityLoadout.loadData(playerData);
        String classId = playerData.getClassId();
        this.playerClass = classId == null ? null : Registries.PLAYER_CLASS.get(classId);
    }

    private void initMechanic() {
        mechanics.add(new OffhandBlockerMechanic());
        mechanics.add(new ClassRestrictionMechanic());
        mechanics.add(new EquipmentStatMechanic());
        mechanics.add(new BaseInteractionMechanic());
        mechanics.add(new EnchantMechanic());
        mechanics.add(new AttributeMechanic());
        mechanics.add(new AbilityLoadoutMechanic());
        mechanics.add(new ProjectileMechanic());
        mechanics.add(new ItemConsumableMechanic());
        mechanics.add(new DurabilityLossMechanic());
        mechanics.add(new ItemComponentMechanic());
        mechanics.add(new ItemAbilityMechanic());

        mechanics.sort(Comparator.comparingInt(PlayerMechanic::getPriority));
    }

    /**
     * Entry point for a slot's item changing - the actual attribute/enchant recomputation lives in
     * {@link EquipmentStatMechanic#onEquipSlotChange}, which runs (for every mechanic, though only
     * that one does anything) before {@link PlayerMechanic#onEquipmentChange} is broadcast, since
     * every other mechanic reacting to that broadcast expects {@link #getActiveAttributes()} /
     * {@link #getActiveEnchants()} to already reflect the change.
     */
    public void updateSlotStat(Player player, EquipSlot slot, @Nullable SmpItem newItem) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onEquipSlotChange(player, slot, newItem, this);
        }
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onEquipmentChange(slot, newItem, this);
        }
    }

    /**
     * Folds {@code delta} into this attribute's active total, pruning the entry once it's
     * effectively zero - used by {@link EquipmentStatMechanic} for both adding a slot's
     * contribution (positive delta) and removing it (negative delta).
     */
    public void mergeAttributeDelta(Attributes attribute, double delta) {
        activeAttributes.merge(attribute, delta, (oldV, d) -> {
            double res = oldV + d;
            return Utils.isEffectiveZero(res) ? null : res;
        });
    }

    /**
     * Folds {@code delta} into this enchant's active level, pruning the entry once it's at or
     * below zero - used by {@link EquipmentStatMechanic} for both adding a slot's contribution
     * (positive delta) and removing it (negative delta).
     */
    public void mergeEnchantDelta(Enchants enchant, int delta) {
        activeEnchants.merge(enchant, delta, (oldVal, d) -> {
            int result = oldVal + d;
            return result <= 0 ? null : result;
        });
    }

    /**
     * Sets (or, if {@code item} is null, clears) what's tracked as equipped in this slot - used by
     * {@link EquipmentStatMechanic}, separately from the attribute/enchant bookkeeping, since a
     * broken item still occupies the slot visually but is tracked here as empty.
     */
    public void setEquipmentSlot(EquipSlot slot, @Nullable SmpItem item) {
        if (item == null) currentEquipment.remove(slot);
        else currentEquipment.put(slot, item);
    }

    public void registerMechanic(PlayerMechanic mechanic) {
        if (!mechanics.contains(mechanic)) {
            mechanics.add(mechanic);
            mechanics.sort(Comparator.comparingInt(PlayerMechanic::getPriority));
        }
    }

    /**
     * Checks if the player has at least the specified amount of money.
     */
    public boolean hasMoney(long amount) {
        if (amount < 0) return false;
        PlayerData data = getPlayerData();
        return data != null && data.getMoney() >= amount;
    }

    /**
     * Attempts to withdraw money from the player.
     * @return true if successful, false if insufficient funds or invalid amount
     */
    public boolean takeMoney(long amount) {
        if (amount <= 0) return false;
        if (!hasMoney(amount)) return false;

        PlayerData data = getPlayerData();
        data.setMoney(data.getMoney() - amount);
        return true;
    }

    /**
     * Gives money to the player with overflow protection.
     */
    public void giveMoney(long amount) {
        if (amount <= 0) return;

        PlayerData data = getPlayerData();
        long current = data.getMoney();

        if (Long.MAX_VALUE - current < amount) {
            data.setMoney(Long.MAX_VALUE);
        } else {
            data.setMoney(current + amount);
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

    public @Nullable PlayerClass getPlayerClass() {
        return playerClass;
    }

    /**
     * Selects (or switches to) a class. Idempotent for abilities already unlocked: only abilities
     * not yet unlocked get granted at the class's default level, so switching back to a
     * previously-held class never touches existing ability levels. Any currently equipped ability
     * that doesn't belong to the new class gets unequipped (its level is untouched - see
     * {@link AbilityLoadout#isAllowedForCurrentClass(String)}).
     */
    public void setPlayerClass(PlayerClass playerClass) {
        PlayerData data = getPlayerData();
        data.setClassId(playerClass.getId());
        this.playerClass = playerClass;

        playerClass.getDefaultAbilities().forEach((abilityId, level) -> {
            if (data.getAbilityLevel(abilityId) <= 0) {
                data.setAbilityLevel(abilityId, level);
            }
        });

        for (AbilityType type : AbilityType.values()) {
            Ability[] equipped = abilityLoadout.getAbilities(type);
            for (int i = 0; i < equipped.length; i++) {
                Ability ability = equipped[i];
                if (ability != null && !playerClass.hasAbility(ability.getId())) {
                    abilityLoadout.equip(type, null, i);
                }
            }
        }
    }

    public @Nullable SmpItem getItemAtEquipSlot(EquipSlot equipSlot) {
        return currentEquipment.get(equipSlot);
    }

    public @Unmodifiable Map<EquipSlot, SmpItem> getCurrentEquipment() {
        return currentEquipmentView;
    }

    /**
     * @param key a caller-chosen unique key, typically {@code abilityTypeId + ":" + itemId}
     */
    public boolean isAbilityOnCooldown(String key) {
        Long expiry = abilityCooldowns.get(key);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public void setAbilityCooldown(String key, long durationMillis) {
        abilityCooldowns.put(key, System.currentTimeMillis() + durationMillis);
    }

    public UUID getUuid() {
        return uuid;
    }

    public PlayerData getPlayerData() {
        return playerData;
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

    public void onEntityInteract(PlayerInteractEntityEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onEntityInteract(event, this);
        }
    }

    public void onArmSwing(PlayerArmSwingEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onArmSwing(event, this);
        }
    }

    public void onDropItem(PlayerDropItemEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onDropItem(event, this);
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

    public void onAbilityCast(AbilityCastEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onAbilityCast(event, this);
        }
    }

    public void onDurabilityChange(DurabilityChangedEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onDurabilityChange(event, this);
        }
    }

    public void onDamageEntity(DamageEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onDamageEntity(event, this);
        }
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onDamageEntityFinal(event, this);
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
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onHurtFinal(event, this);
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

    public void onTeleport(PlayerTeleportEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onTeleport(event, this);
        }
    }

    public void onDeath(PlayerDeathEvent event) {
        for (PlayerMechanic mechanic : mechanics) {
            mechanic.onDeath(event, this);
        }
    }
}
