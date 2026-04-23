package com.roguesmp.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.EntityAttribute;
import com.roguesmp.constant.Keys;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.registry.entity.EntityRegistry;
import com.roguesmp.registry.entity.EntitySpellRegistry;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Represent the data of our smpEntity
 */
public class BaseEntity {
    private final String id; // ID should remain final as it's the unique identifier
    private final EntityType entityType; // Changing the physical type of entity usually requires a respawn

    // Converted to non-final for dynamic updates
    private String displayName;
    private boolean noAi;
    private boolean invulnerable;
    private boolean persistent;
    private boolean isBoss;
    private boolean isElite;
    private int detectionRange;

    // Collections are usually final (the reference), but the contents are mutable.
    // However, if you want to swap the entire map at once, make them non-final.
    private Map<EntityAttribute, Double> baseStat;
    private Map<EquipmentSlot, EntityEquipment> equipments;

    private List<String> activeSpell;
    private List<String> passiveSpell;
    private int passiveInterval;
    private boolean canCastSameSpellTwice;
    private Map<String, Map<String, Object>> spellParams;

    public BaseEntity(String id, EntityType entityType) {
        this.id = id;
        this.entityType = entityType;
    }

    public BaseEntity(String id, Map<EntityAttribute, Double> baseStat, Map<EquipmentSlot, EntityEquipment> equipments, EntityType entityType, String displayName, boolean noAi, boolean invulnerable, boolean persistent, boolean isBoss, boolean isElite, int detectionRange, List<String> activeSpell, List<String> passiveSpell, int passiveInterval, boolean canCastSameSpellTwice, Map<String, Map<String, Object>> spellParams) {
        this.id = id;
        this.entityType = entityType;
        this.displayName = displayName;
        this.noAi = noAi;
        this.invulnerable = invulnerable;
        this.persistent = persistent;
        this.isBoss = isBoss;
        this.isElite = isElite;
        this.detectionRange = detectionRange;
        this.activeSpell = activeSpell;
        this.passiveSpell = passiveSpell;
        this.passiveInterval = passiveInterval;
        this.canCastSameSpellTwice = canCastSameSpellTwice;
        this.spellParams = spellParams;
        this.baseStat = baseStat;
        this.equipments = equipments;
    }

    /**
     * Spawn entity and start its spells
     * @param location location
     * @return SmpEntity instance
     */
    public SmpEntity spawn(Location location) {
        Entity spawned = location.getWorld().spawn(location, this.getEntityType().getEntityClass(), false, entity -> {});

        if (!(spawned instanceof LivingEntity living)) {
            throw new RuntimeException("EntityType must be a living entity!");
        }
        SmpEntity smpEntity = EntityRegistry.getInstance().wrap(this, living);
        smpEntity.initialize();
        EntityManager.getInstance().register(smpEntity);

        return smpEntity;
    }

    public EntitySnapshot spawnOnlyEquipmentSnapshot(Location location) {
        Entity entity = location.getWorld().createEntity(location, this.getEntityType().getEntityClass());
        processEntity(entity);
        if (!(entity instanceof LivingEntity living)) {
            RogueSmpCore.LOGGER.warn("EntityType must be a living entity!");
            throw new RuntimeException("EntityType must be a living entity!");
        }
        EntitySnapshot snapshot = living.createSnapshot();
        living.remove();
        return snapshot;
    }

    /**
     * If this SmpEntity is not initialized, add spells from BaseEntity to this SmpEntity instance
     * @param smpEntity the entity
     */
    public void processSpell(SmpEntity smpEntity) {
        if (smpEntity.isInitialized()) return;
        List<Spell> activeSpells = new ArrayList<>();
        if (activeSpell != null) {
            activeSpell.forEach(s -> {
                Map<String, Object> param;
                if (spellParams == null) param = null;
                else param = spellParams.get(s);
                Spell spell = EntitySpellRegistry.createSpell(s, param, smpEntity.entity);
                if (spell == null) return;
                activeSpells.add(spell);
            });
        }
        SpellManager spellManager = new SpellManager(activeSpells);

        List<Spell> passiveSpells = new ArrayList<>();
        if (passiveSpell != null) {
            passiveSpell.forEach(s -> {
                Map<String, Object> param;
                if (spellParams == null) param = null;
                else param = spellParams.get(s);
                Spell spell = EntitySpellRegistry.createSpell(s, param, smpEntity.entity);
                if (spell == null) return;
                passiveSpells.add(spell);
            });
        }
        int passiveIntervalTick = passiveInterval <= 0 ? SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT : passiveInterval;
        int detectionRange2 = detectionRange <= 0 ? 20 : detectionRange;
        smpEntity.startSpell(spellManager, passiveSpells, detectionRange2, null, 1, passiveIntervalTick, canCastSameSpellTwice);
    }

    public void processEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;

        living.setInvulnerable(invulnerable);
        living.setAI(!noAi);
        living.setPersistent(persistent);
        living.setRemoveWhenFarAway(!persistent);
        living.setCustomNameVisible(true);
        living.customName(Utils.fromString(displayName));
        PersistentDataContainer pdc = living.getPersistentDataContainer();
        pdc.set(Keys.MOB_ID, PersistentDataType.STRING, id);
        if (baseStat != null) {
            this.getBaseStat().forEach((entityAttribute, aDouble) -> {
                AttributeInstance instance = living.getAttribute(entityAttribute.getBukkitAttribute());
                if (instance == null) {
                    living.registerAttribute(entityAttribute.getBukkitAttribute());
                }
                // Cannot be null since we registered it
                instance.setBaseValue(aDouble);
                if (entityAttribute.getBukkitAttribute() == Attribute.MAX_HEALTH) living.setHealth(aDouble);
            });
        }


        org.bukkit.inventory.EntityEquipment equipment = living.getEquipment();
        if (equipment != null) {
            if (equipments != null) {
                this.getEquipments().forEach((equipmentSlot, entityEquipment) -> {
                    equipment.setDropChance(equipmentSlot, 0f);
                    equipment.setItem(equipmentSlot, entityEquipment.createItemStack());
                });
            }

        }
    }

    public String getId() {
        return id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public @Unmodifiable Map<EntityAttribute, Double> getBaseStat() {
        return Map.copyOf(baseStat);
    }

    public @Unmodifiable Map<EquipmentSlot, EntityEquipment> getEquipments() {
        return Map.copyOf(equipments);
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isBoss() {
        return isBoss;
    }

    public boolean isElite() {
        return isElite;
    }

    public int getDetectionRange() {
        return detectionRange;
    }

    public List<String> getActiveSpell() {
        return activeSpell;
    }

    public List<String> getPassiveSpell() {
        return passiveSpell;
    }

    public int getPassiveInterval() {
        return passiveInterval;
    }

    public boolean isCanCastSameSpellTwice() {
        return canCastSameSpellTwice;
    }

    public Map<String, Map<String, Object>> getSpellParams() {
        return spellParams;
    }

    public BaseEntity setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public BaseEntity setNoAi(boolean noAi) {
        this.noAi = noAi;
        return this;
    }

    public BaseEntity setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
        return this;
    }

    public BaseEntity setPersistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public BaseEntity setBoss(boolean boss) {
        isBoss = boss;
        return this;
    }

    public BaseEntity setElite(boolean elite) {
        isElite = elite;
        return this;
    }

    public BaseEntity setDetectionRange(int detectionRange) {
        this.detectionRange = detectionRange;
        return this;
    }

    public BaseEntity setBaseStat(Map<EntityAttribute, Double> baseStat) {
        this.baseStat = baseStat;
        return this;
    }

    public BaseEntity setEquipments(Map<EquipmentSlot, EntityEquipment> equipments) {
        this.equipments = equipments;
        return this;
    }

    public BaseEntity setActiveSpell(List<String> activeSpell) {
        this.activeSpell = activeSpell;
        return this;
    }

    public BaseEntity setPassiveSpell(List<String> passiveSpell) {
        this.passiveSpell = passiveSpell;
        return this;
    }

    public BaseEntity setPassiveInterval(int passiveInterval) {
        this.passiveInterval = passiveInterval;
        return this;
    }

    public BaseEntity setCanCastSameSpellTwice(boolean canCastSameSpellTwice) {
        this.canCastSameSpellTwice = canCastSameSpellTwice;
        return this;
    }

    public BaseEntity setSpellParams(Map<String, Map<String, Object>> spellParams) {
        this.spellParams = spellParams;
        return this;
    }
}
