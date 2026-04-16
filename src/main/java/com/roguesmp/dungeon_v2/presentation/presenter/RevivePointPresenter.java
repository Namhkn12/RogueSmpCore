package com.roguesmp.dungeon_v2.presentation.presenter;

import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

public class RevivePointPresenter {

    private static final double GROUND_OFFSET = -0.35D;
    private static final double FLOAT_AMPLITUDE = 0.01D;

    public ArmorStand spawnRevivePoint(Player deadPlayer, Location deadLocation) {
        if (deadPlayer == null || deadLocation == null || deadLocation.getWorld() == null) {
            return null;
        }

        EntityEquipment playerEq = deadPlayer.getEquipment();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(deadPlayer);
            head.setItemMeta(skullMeta);
        }

        ItemStack chestplate = getOrDefault(playerEq.getChestplate(), Material.CHAINMAIL_CHESTPLATE);
        ItemStack leggings = getOrDefault(playerEq.getLeggings(), Material.CHAINMAIL_LEGGINGS);
        ItemStack boots = getOrDefault(playerEq.getBoots(), Material.CHAINMAIL_BOOTS);
        ItemStack mainHand = getHand(playerEq.getItemInMainHand());
        ItemStack offHand = getHand(playerEq.getItemInOffHand());

        Location spawnLoc = deadLocation.clone().add(0, GROUND_OFFSET, 0);

        return spawnLoc.getWorld().spawn(spawnLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setCanPickupItems(false);
            stand.setCollidable(false);
            stand.setSilent(true);
            stand.setArms(true);
            stand.setBasePlate(false);
            stand.setSmall(false);

            stand.setBodyPose(new EulerAngle(Math.toRadians(271), 0, 0));
            stand.setHeadPose(new EulerAngle(Math.toRadians(276), 0, 0));
            stand.setRightArmPose(new EulerAngle(Math.toRadians(272), 0, 0));
            stand.setLeftArmPose(new EulerAngle(Math.toRadians(272), 0, 0));

            // Keep legs neutral so they remain attached to the torso in lie-down pose.
            stand.setLeftLegPose(new EulerAngle(0, 0, 0));
            stand.setRightLegPose(new EulerAngle(0, 0, 0));

            stand.setGlowing(true);
            stand.setRotation(deadLocation.getYaw(), 0);

            EntityEquipment eq = stand.getEquipment();
            eq.setHelmet(head);
            eq.setChestplate(chestplate);
            eq.setLeggings(leggings);
            eq.setBoots(boots);
            eq.setItemInMainHand(mainHand);
            eq.setItemInOffHand(offHand);

            stand.getPersistentDataContainer().set(
                    NameSpaceKeys.REVIVE_POINT_KEY,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        });
    }

    public void tickRevivePoint(ArmorStand stand, Location center, int tick) {
        if (stand == null || stand.isDead() || center == null || center.getWorld() == null) {
            return;
        }

        double floatY = Math.sin(tick / 10.0) * FLOAT_AMPLITUDE;
        Location floatLoc = center.clone().add(0, GROUND_OFFSET + floatY, 0);

        stand.teleport(floatLoc);

        if (tick % 4 == 0) {
            center.getWorld().spawnParticle(
                    Particle.ENCHANT,
                    floatLoc.clone().add(0, 1.1, 0),
                    6,
                    0.3, 0.25, 0.3,
                    0
            );
        }
    }

    public void removeRevivePoint(ArmorStand stand) {
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    private ItemStack getHand(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return item.clone();
    }

    private ItemStack getOrDefault(ItemStack item, Material fallback) {
        if (item == null || item.getType().isAir()) {
            return new ItemStack(fallback);
        }
        return item.clone();
    }
}
