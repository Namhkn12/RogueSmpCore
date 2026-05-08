package com.roguesmp.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.roguesmp.constant.Keys;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BaseNpc {
    private final EntityType entityType;
    private final String name;
    private final String skinValue;
    private final String skinSignature;
    private final String description;
    private final String id;
    private final List<NpcAction> actions;

    public BaseNpc(EntityType entityType, String name, String skinValue, String skinSignature, String description, String id, List<NpcAction> actions) {
        this.entityType = entityType;
        this.name = name;
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
        this.description = description;
        this.id = id;
        this.actions = actions;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getId() {
        return id;
    }

    public List<NpcAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public Entity spawn(Location location) {

        return location.getWorld().spawn(location, entityType.getEntityClass(), this::processEntity);
    }

    public void processEntity(Entity entity) {
        entity.getPersistentDataContainer().set(Keys.NPC_ID, PersistentDataType.STRING, id);
        entity.setPersistent(true);
        entity.customName(Utils.fromString(name));
        entity.setCustomNameVisible(true);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setInvulnerable(true);
            if (entity instanceof Mannequin mannequin) {
                mannequin.setDescription(Utils.fromString(description));
                mannequin.setImmovable(true);

                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
                profile.setProperty(new ProfileProperty("textures", skinValue, skinSignature));
                mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
            }
        }
    }
}
