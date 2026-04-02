package com.roguesmp.constant;

import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum EquipSlot {
    MAINHAND(EquipmentSlotGroup.MAINHAND, "<!i><gray>Khi trang bị tay chính:", "mainhand"),
    OFFHAND(EquipmentSlotGroup.OFFHAND, "<!i><gray>Khi trang bị tay phụ:", "offhand"),
    HEAD(EquipmentSlotGroup.HEAD, "<!i><gray>Khi trang bị ở đầu:", "head"),
    CHEST(EquipmentSlotGroup.CHEST, "<!i><gray>Khi trang bị ở ngực:", "chest"),
    LEGS(EquipmentSlotGroup.LEGS, "<!i><gray>Khi trang bị ở chân:", "legs"),
    FEET(EquipmentSlotGroup.FEET, "<!i><gray>Khi trang bị ở bàn chân:", "feet"),
    PROJECTILE(null, "<!i><gray>Khi bắn:", "projectile");

    private static final Map<EquipmentSlotGroup, EquipSlot> vanillaToSlot = new HashMap<>();

    private final EquipmentSlotGroup vanillaSlot;
    private final String displayString;
    private final String id;

    EquipSlot(EquipmentSlotGroup vanillaSlot, String displayString, String id) {
        this.vanillaSlot = vanillaSlot;
        this.displayString = displayString;
        this.id = id;
    }

    public static EquipSlot fromVanilla(EquipmentSlotGroup slotGroup) {
        return vanillaToSlot.get(slotGroup);
    }

    public @Nullable EquipmentSlotGroup getVanillaSlot() {
        return vanillaSlot;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getId() {
        return id;
    }

    static {
        for (EquipSlot slot : EquipSlot.values()) {
            vanillaToSlot.put(slot.vanillaSlot, slot);
        }
    }
}
