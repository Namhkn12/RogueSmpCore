package com.roguesmp.dungeon_v2.presentation.presenter;

import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class RevivePointPresenter {

    public Mannequin spawnRevivePoint(Player deadPlayer, Location deadLocation) {
        if (deadPlayer == null || deadLocation == null || deadLocation.getWorld() == null) {
            return null;
        }

        EntityEquipment playerEq = deadPlayer.getEquipment();

        ItemStack hemet = getItemFromPlayer(playerEq.getHelmet());
        ItemStack chestplate = getItemFromPlayer(playerEq.getChestplate());
        ItemStack leggings = getItemFromPlayer(playerEq.getLeggings());
        ItemStack boots = getItemFromPlayer(playerEq.getBoots());
        ItemStack mainHand = getItemFromPlayer(playerEq.getItemInMainHand());
        ItemStack offHand = getItemFromPlayer(playerEq.getItemInOffHand());

        Location spawnLoc = deadLocation.clone();
        spawnLoc.add(0, 0.2, 0);

        return spawnLoc.getWorld().spawn(spawnLoc, Mannequin.class, mannequin -> {

            mannequin.setProfile(
                    ResolvableProfile.resolvableProfile(deadPlayer.getPlayerProfile())
            );

            mannequin.setPose(Pose.SLEEPING);

            mannequin.setGlowing(true);
            mannequin.setGravity(false);
            mannequin.setSilent(true);
            mannequin.setInvulnerable(true);
            mannequin.setCollidable(false);
            mannequin.setAI(false);
            mannequin.setNoPhysics(true);
            mannequin.setImmovable(true);
            mannequin.setRemoveWhenFarAway(false);

            mannequin.setRotation(deadLocation.getYaw(), 0);

            EntityEquipment eq = mannequin.getEquipment();
            eq.setHelmet(hemet);
            eq.setChestplate(chestplate);
            eq.setLeggings(leggings);
            eq.setBoots(boots);
            eq.setItemInMainHand(mainHand);
            eq.setItemInOffHand(offHand);

            mannequin.getPersistentDataContainer().set(
                    NameSpaceKeys.REVIVE_POINT_KEY,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        });
    }

    public void tickRevivePoint(Mannequin mannequin, Location center, int tick) {
        if (mannequin == null || mannequin.isDead() || center == null || center.getWorld() == null) {
            return;
        }

//        double floatY = Math.sin(tick / 10.0) * FLOAT_AMPLITUDE;
//        Location floatLoc = center.clone().add(0, GROUND_OFFSET + floatY, 0);
//
//        stand.teleport(floatLoc);
//
//        if (tick % 4 == 0) {
//            center.getWorld().spawnParticle(
//                    Particle.ENCHANT,
//                    floatLoc.clone().add(0, 1.1, 0),
//                    6,
//                    0.3, 0.25, 0.3,
//                    0
//            );
//        }
    }

    public void removeRevivePoint(Mannequin mannequin) {
        if (mannequin != null && !mannequin.isDead()) {
            mannequin.remove();
        }
    }

    private ItemStack getItemFromPlayer(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return item.clone();
    }

}