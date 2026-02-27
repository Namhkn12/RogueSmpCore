package com.roguesmp.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.EntityAttribute;
import com.roguesmp.constant.Keys;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.registry.EntitySpellRegistry;
import org.bukkit.Location;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
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
    private final String id;
    private final EntityType entityType;
    private final String displayName;
    private final boolean persistent;
    private final boolean isBoss;
    private final boolean isElite;
    private final int detectionRange;
    private final Map<EntityAttribute, Double> baseStat = new EnumMap<>(EntityAttribute.class);
    private final Map<EquipmentSlot, EntityEquipment> equipments = new EnumMap<>(EquipmentSlot.class);
    private final List<String> activeSpell;
    private final List<String> passiveSpell;
    private final int passiveInterval;
    private final boolean canCastSameSpellTwice;

    private final Map<String, Map<String, Object>> spellParams;

    public BaseEntity(String id, Map<EntityAttribute, Double> baseStat, Map<EquipmentSlot, EntityEquipment> equipments, EntityType entityType, String displayName, boolean persistent, boolean isBoss, boolean isElite, int detectionRange, List<String> activeSpell, List<String> passiveSpell, int passiveInterval, boolean canCastSameSpellTwice, Map<String, Map<String, Object>> spellParams) {
        this.id = id;
        this.entityType = entityType;
        this.displayName = displayName;
        this.persistent = persistent;
        this.isBoss = isBoss;
        this.isElite = isElite;
        this.detectionRange = detectionRange;
        this.activeSpell = activeSpell;
        this.passiveSpell = passiveSpell;
        this.passiveInterval = passiveInterval;
        this.canCastSameSpellTwice = canCastSameSpellTwice;
        this.spellParams = spellParams;
        this.baseStat.putAll(baseStat);
        this.equipments.putAll(equipments);
    }

    public SmpEntity spawn(Location location, boolean startSpell) {
        Entity spawned = location.getWorld().spawn(location, this.getEntityType().getEntityClass(), entity -> {
            if (!(entity instanceof LivingEntity living)) return;

            living.setPersistent(persistent);
            PersistentDataContainer pdc = living.getPersistentDataContainer();
            pdc.set(Keys.MOB_ID, PersistentDataType.STRING, id);

            this.getBaseStat().forEach((entityAttribute, aDouble) -> {
                AttributeInstance instance = living.getAttribute(entityAttribute.getBukkitAttribute());
                if (instance == null) {
                    living.registerAttribute(entityAttribute.getBukkitAttribute());
                }
                // Cannot be null since we registered it
                instance.setBaseValue(aDouble);
            });

            org.bukkit.inventory.EntityEquipment equipment = living.getEquipment();
            if (equipment != null) {
                this.getEquipments().forEach((equipmentSlot, entityEquipment) -> {
                    equipment.setItem(equipmentSlot, entityEquipment.createItemStack());
                });
            }

        });
        if (!(spawned instanceof LivingEntity living)) {
            RogueSmpCore.LOGGER.warn("EntityType must be a living entity!");
            throw new RuntimeException("EntityType must be a living entity!");
        }

        SmpEntity smpEntity = new SmpEntity(this, living);
        EntityManager.getInstance().load(smpEntity);

        if (!startSpell) return smpEntity;
        List<Spell> activeSpells = new ArrayList<>();
        if (activeSpell != null) {
            activeSpell.forEach(s -> {
                Map<String, Object> param = spellParams.get(s);
                Spell spell;
                if (param == null) {
                    spell = EntitySpellRegistry.getSpellNoParam(s);
                } else spell = EntitySpellRegistry.getSpellParam(s, param);
                if (spell == null) return;
                activeSpells.add(spell);
            });
        }
        SpellManager spellManager = new SpellManager(activeSpells);

        List<Spell> passiveSpells = new ArrayList<>();
        if (passiveSpell != null) {
            passiveSpell.forEach(s -> {
                Map<String, Object> param = spellParams.get(s);
                Spell spell;
                if (param == null) {
                    spell = EntitySpellRegistry.getSpellNoParam(s);
                } else spell = EntitySpellRegistry.getSpellParam(s, param);
                if (spell == null) return;
                passiveSpells.add(spell);
            });
        }
        int passiveIntervalTick = passiveInterval <= 0 ? 2 : passiveInterval;
        smpEntity.startSpell(spellManager, passiveSpells, detectionRange, null, 5, passiveIntervalTick, canCastSameSpellTwice);

        return smpEntity;
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
}
