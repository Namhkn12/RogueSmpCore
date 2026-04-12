package com.roguesmp.constant;

import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum EquipSlot {
    MAINHAND(EquipmentSlotGroup.MAINHAND, "<!i><gray>Khi trang bị tay chính:", "Tay chính","mainhand"),
    OFFHAND(EquipmentSlotGroup.OFFHAND, "<!i><gray>Khi trang bị tay phụ:", "Tay phụ", "offhand"),
    HEAD(EquipmentSlotGroup.HEAD, "<!i><gray>Khi trang bị ở đầu:", "Đầu", "head"),
    CHEST(EquipmentSlotGroup.CHEST, "<!i><gray>Khi trang bị ở ngực:", "Ngực", "chest"),
    LEGS(EquipmentSlotGroup.LEGS, "<!i><gray>Khi trang bị ở chân:", "Chân", "legs"),
    FEET(EquipmentSlotGroup.FEET, "<!i><gray>Khi trang bị ở bàn chân:", "Bàn chân","feet"),
    PROJECTILE(null, "<!i><gray>Khi bắn:", "Đạn", "projectile");

    private static final Map<EquipmentSlotGroup, EquipSlot> vanillaToSlot = new HashMap<>();

    private final EquipmentSlotGroup vanillaSlot;
    private final String displayString;
    private final String simpleName;
    private final String id;

    EquipSlot(EquipmentSlotGroup vanillaSlot, String displayString, String simpleName, String id) {
        this.vanillaSlot = vanillaSlot;
        this.displayString = displayString;
        this.simpleName = simpleName;
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

    public String getSimpleName() {
        return simpleName;
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
