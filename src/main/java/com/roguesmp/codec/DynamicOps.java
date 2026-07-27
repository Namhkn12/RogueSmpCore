package com.roguesmp.codec;

import java.util.List;
import java.util.Map;

/**
 * Handles converting data to and from a specific format like JSON, NBT, or YAML.
 *
 * @param <O> the object type used by the target format (e.g., JsonElement)
 */
public interface DynamicOps<O> {

    /**
     * Creates a new, empty map or object in this format.
     *
     * @return an empty map
     */
    O emptyMap();

    /**
     * Converts a text string into this format.
     *
     * @param value the string to convert
     * @return the formatted string object
     */
    O createString(String value);

    /**
     * Converts a boolean value into this format.
     *
     * @param value the boolean to convert
     * @return the formatted boolean object
     */
    O createBoolean(boolean value);

    /**
     * Converts a byte into this format.
     *
     * @param value the byte to convert
     * @return the formatted byte object
     */
    O createByte(byte value);

    /**
     * Converts a short into this format.
     *
     * @param value the short to convert
     * @return the formatted short object
     */
    O createShort(short value);

    /**
     * Converts an integer into this format.
     *
     * @param value the number to convert
     * @return the formatted integer object
     */
    O createInt(int value);

    /**
     * Converts a long into this format.
     *
     * @param value the long to convert
     * @return the formatted long object
     */
    O createLong(long value);

    /**
     * Converts a float into this format.
     *
     * @param value the float to convert
     * @return the formatted float object
     */
    O createFloat(float value);

    /**
     * Converts a decimal number into this format.
     *
     * @param value the double to convert
     * @return the formatted double object
     */
    O createDouble(double value);

    /**
     * Converts a list of values into a formatted list object.
     *
     * @param elements the items to put in the list
     * @return the formatted list object
     */
    O createList(List<O> elements);

    /**
     * Reads a text string from the input.
     *
     * @param input the data to read
     * @return a result with the string, or an error if it isn't a string
     */
    DataResult<String> getString(O input);

    /**
     * Reads a boolean from the input.
     *
     * @param input the data to read
     * @return a result with the boolean, or an error if it isn't a boolean
     */
    DataResult<Boolean> getBoolean(O input);

    /**
     * Reads a byte from the input.
     *
     * @param input the data to read
     * @return a result with the byte, or an error if it isn't a byte
     */
    DataResult<Byte> getByte(O input);

    /**
     * Reads a short from the input.
     *
     * @param input the data to read
     * @return a result with the short, or an error if it isn't a short
     */
    DataResult<Short> getShort(O input);

    /**
     * Reads an integer from the input.
     *
     * @param input the data to read
     * @return a result with the integer, or an error if it isn't an integer
     */
    DataResult<Integer> getInt(O input);

    /**
     * Reads a long from the input.
     *
     * @param input the data to read
     * @return a result with the long, or an error if it isn't a long
     */
    DataResult<Long> getLong(O input);

    /**
     * Reads a float from the input.
     *
     * @param input the data to read
     * @return a result with the float, or an error if it isn't a float
     */
    DataResult<Float> getFloat(O input);

    /**
     * Reads a double from the input.
     *
     * @param input the data to read
     * @return a result with the double, or an error if it isn't a double
     */
    DataResult<Double> getDouble(O input);

    /**
     * Reads a list of items from the input.
     *
     * @param input the data to read
     * @return a result with the list, or an error if it isn't a list
     */
    DataResult<List<O>> getList(O input);

    /**
     * Reads all key-value entries from a map object.
     *
     * @param input the data to read
     * @return a result with the map entries, or an error if it isn't a map
     */
    DataResult<Map<String, O>> getMap(O input);

    /**
     * Looks up a value inside a map using a key name.
     *
     * @param mapInput the map to search
     * @param key the name of the field to find
     * @return a result with the value, or an error if the key doesn't exist
     */
    DataResult<O> getMapField(O mapInput, String key);

    /**
     * Adds or updates a key-value pair inside a map.
     *
     * @param mapInput the map to modify
     * @param key the field name to set
     * @param value the value to store at the key
     * @return the updated map
     */
    O setMapEntry(O mapInput, String key, O value);
}
