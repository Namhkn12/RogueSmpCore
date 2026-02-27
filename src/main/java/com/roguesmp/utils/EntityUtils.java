package com.roguesmp.utils;

import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

public class EntityUtils {
    public static double getMaxHealth(Attributable attributable) {
        AttributeInstance maxHealth = attributable.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 0 : maxHealth.getValue();
    }
}
