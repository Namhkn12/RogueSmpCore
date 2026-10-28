package com.roguesmp.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.constant.Keys;
import com.roguesmp.entity.EntityEquipment;
import com.roguesmp.action.ActionEntry;
import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.utils.Utils;
import com.roguesmp.RogueSmpCore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Display;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaseNpc {

    private static final Codec<List<String>> DESCRIPTION_CODEC = Codec.withAlternative(
            Codec.listOf(Codec.STRING),
            Codec.STRING.xmap(List::of, lines -> String.join("\n", lines))
    );

    private static final Codec<Villager.Profession> PROFESSION_CODEC = Codec.STRING.comapFlatMap(
            raw -> {
                NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase());
                Villager.Profession profession = key == null ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.VILLAGER_PROFESSION).get(key);
                return profession == null ? DataResult.error("Invalid villager profession: " + raw) : DataResult.success(profession);
            },
            profession -> profession.key().asString()
    );

    private static final Codec<Map<EquipmentSlot, EntityEquipment>> EQUIPMENT_CODEC =
            Codec.lenientUnboundedMap(Codec.enumOf(EquipmentSlot.class), EntityEquipment.CODEC);

    private static final float LABEL_OFFSET = 0.3f;
    private static final float LINE_HEIGHT = 0.27f;

    public static final Codec<BaseNpc> CODEC = Codec.composite(
            Codec.enumOf(EntityType.class).fieldOf("entityType").forGetter(BaseNpc::getEntityType),
            Codec.STRING.optionalFieldOf("name", () -> null).forGetter(BaseNpc::getName),
            Codec.STRING.optionalFieldOf("skinValue", () -> null).forGetter(BaseNpc::getSkinValue),
            Codec.STRING.optionalFieldOf("skinSignature", () -> null).forGetter(BaseNpc::getSkinSignature),
            Codec.STRING.optionalFieldOf("skinId", () -> null).forGetter(BaseNpc::getSkinId),
            DESCRIPTION_CODEC.optionalFieldOf("description", List::of).forGetter(BaseNpc::getDescription),
            PROFESSION_CODEC.optionalFieldOf("profession", () -> null).forGetter(BaseNpc::getProfession),
            EQUIPMENT_CODEC.optionalFieldOf("equipment", Map::of).forGetter(BaseNpc::getEquipment),
            Codec.STRING.fieldOf("id").forGetter(BaseNpc::getId),
            Codec.listOf(ActionEntry.CODEC).fieldOf("actions").forGetter(BaseNpc::getActions),
            BaseNpc::new
    );

    private final EntityType entityType;
    private final String name;
    private final String skinValue;
    private final String skinSignature;
    private final String skinId;
    private final List<String> description;
    private final Villager.Profession profession;
    private final Map<EquipmentSlot, EntityEquipment> equipment;
    private final String id;
    private final List<ActionEntry> actions;

    public BaseNpc(EntityType entityType, String name, String skinValue, String skinSignature, String skinId, List<String> description, Villager.Profession profession, Map<EquipmentSlot, EntityEquipment> equipment, String id, List<ActionEntry> actions) {
        this.entityType = entityType;
        this.name = name;
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
        this.skinId = skinId;
        this.description = description;
        this.profession = profession;
        this.equipment = equipment;
        this.id = id;
        this.actions = actions;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSkinValue() {
        return skinValue;
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    public @Unmodifiable List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public Villager.Profession getProfession() {
        return profession;
    }

    public @Unmodifiable Map<EquipmentSlot, EntityEquipment> getEquipment() {
        return Collections.unmodifiableMap(equipment);
    }

    public String getSkinId() {
        return skinId;
    }

    public List<ActionEntry> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public Entity spawn(Location location) {

        return location.getWorld().spawn(location, entityType.getEntityClass(), this::processEntity);
    }

    public void processEntity(Entity entity) {
        entity.getPersistentDataContainer().set(Keys.NPC_ID, PersistentDataType.STRING, id);
        entity.setPersistent(true);
        entity.customName(Utils.fromString(name));
        entity.setCustomNameVisible(false);
        entity.getScheduler().run(RogueSmpCore.getInstance(), _ -> attachLabel(entity), null);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setInvulnerable(true);
            org.bukkit.inventory.EntityEquipment slots = living.getEquipment();
            if (slots != null) {
                slots.clear();
                equipment.forEach((slot, item) -> {
                    slots.setItem(slot, item.createItemStack());
                    slots.setDropChance(slot, 0f);
                });
            }
            if (entity instanceof Villager villager && profession != null) {
                villager.setProfession(profession);
            }
            if (entity instanceof Mannequin mannequin) {
                mannequin.setDescription(null);
                mannequin.setImmovable(true);

                if (skinValue != null && skinSignature != null) {
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
                    profile.setProperty(new ProfileProperty("textures", skinValue, skinSignature));
                    mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
                } else if (skinId != null) {
                    SkinRegistry.SkinData skinData = SkinRegistry.getInstance().getSkin(skinId);
                    if (skinData != null) mannequin.setProfile(ResolvableProfile.resolvableProfile(skinData.getProfile()));
                }

            }
        }
    }

    private void attachLabel(Entity entity) {
        entity.getPassengers().stream().filter(TextDisplay.class::isInstance).forEach(Entity::remove);

        if (!description.isEmpty()) {
            spawnLabel(entity, Component.join(JoinConfiguration.newlines(), Utils.fromStrings(description)), LABEL_OFFSET);
        }
        if (name != null) {
            spawnLabel(entity, Utils.fromString(name), LABEL_OFFSET + description.size() * LINE_HEIGHT);
        }
    }

    private static void spawnLabel(Entity entity, Component content, float yOffset) {
        TextDisplay display = entity.getWorld().spawn(entity.getLocation(), TextDisplay.class, text -> {
            text.text(content);
            text.setLineWidth(Integer.MAX_VALUE);
            text.setBillboard(Display.Billboard.CENTER);
            text.setPersistent(false);
            text.setTransformation(new Transformation(
                    new Vector3f(0, yOffset, 0), new Quaternionf(), new Vector3f(1), new Quaternionf()));
        });
        entity.addPassenger(display);
    }
}
