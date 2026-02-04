package com.roguesmp.damage;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;

public record DamageModifier(String source, double value, DamageType type, DamageOperation operation) {
}
