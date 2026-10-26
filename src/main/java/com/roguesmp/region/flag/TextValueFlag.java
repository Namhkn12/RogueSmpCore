package com.roguesmp.region.flag;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import org.jetbrains.annotations.Nullable;

public abstract class TextValueFlag<T> extends Flag<T> {

    protected TextValueFlag(String name) {
        super(name);
    }

    protected abstract T parse(String[] args) throws InvalidFlagFormat;

    protected abstract String format(T value);

    @Override
    public T parseInput(FlagContext context) throws InvalidFlagFormat {
        String input = context.getUserInput().trim();
        if (input.isEmpty()) throw new InvalidFlagFormat("Value cannot be empty.");
        return parse(input.split("\\s+"));
    }

    @Override
    public @Nullable T unmarshal(@Nullable Object object) {
        if (object == null) return null;
        try {
            return parse(object.toString().trim().split("\\s+"));
        } catch (InvalidFlagFormat e) {
            RogueSmpCore.LOGGER.warn("Dropping stored value '{}' of region flag {}: {}", object, getName(), e.getMessage());
            return null;
        }
    }

    @Override
    public Object marshal(T value) {
        return format(value);
    }

    protected static double parseDouble(String text, String field) throws InvalidFlagFormat {
        Double value = Utils.parseDouble(text);
        if (value == null) throw new InvalidFlagFormat("'" + text + "' is not a valid " + field + ".");
        return value;
    }

    protected static int parseInt(String text, String field) throws InvalidFlagFormat {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidFlagFormat("'" + text + "' is not a valid " + field + ".");
        }
    }
}
