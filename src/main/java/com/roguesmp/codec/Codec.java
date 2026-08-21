package com.roguesmp.codec;

import com.roguesmp.RogueSmpCore;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Converts Java objects to and from serialized data (like JSON or NBT).
 *
 * @param <A> the Java object type this codec handles
 */
public interface Codec<A> {

    /**
     * Converts a Java object into serialized data.
     *
     * @param <O> the target data format type
     * @param input the object to encode
     * @param ops the format helper
     * @return a result with the encoded data, or an error
     */
    <O> DataResult<O> encode(A input, DynamicOps<O> ops);

    /**
     * Reads serialized data back into a Java object.
     *
     * @param <O> the input data format type
     * @param input the raw data to decode
     * @param ops the format helper
     * @return a result with the decoded object, or an error
     */
    <O> DataResult<A> decode(O input, DynamicOps<O> ops);

    /**
     * Builds a detailed "field missing" message that also lists which fields ARE present,
     * so a typo'd/renamed field name is obvious from the log line alone.
     */
    private static <O> String missingFieldMessage(String name, O inputMap, DynamicOps<O> ops) {
        DataResult<Map<String, O>> raw = ops.getMap(inputMap);
        if (raw.isSuccess()) {
            return "Missing field '" + name + "' (available fields: " + raw.result().keySet() + ")";
        }
        return "Missing field '" + name + "' (input is not an object: " + inputMap + ")";
    }

    // --- PRIMITIVES ---

    /** Codec for text strings. */
    Codec<String> STRING = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(String input, DynamicOps<O> ops) {
            return DataResult.success(ops.createString(input));
        }

        @Override
        public <O> DataResult<String> decode(O input, DynamicOps<O> ops) {
            return ops.getString(input);
        }
    };

    /** Codec for boolean values. */
    Codec<Boolean> BOOLEAN = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Boolean input, DynamicOps<O> ops) {
            return DataResult.success(ops.createBoolean(input));
        }

        @Override
        public <O> DataResult<Boolean> decode(O input, DynamicOps<O> ops) {
            return ops.getBoolean(input);
        }
    };

    /** Codec for byte numbers. */
    Codec<Byte> BYTE = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Byte input, DynamicOps<O> ops) {
            return DataResult.success(ops.createByte(input));
        }

        @Override
        public <O> DataResult<Byte> decode(O input, DynamicOps<O> ops) {
            return ops.getByte(input);
        }
    };

    /** Codec for short numbers. */
    Codec<Short> SHORT = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Short input, DynamicOps<O> ops) {
            return DataResult.success(ops.createShort(input));
        }

        @Override
        public <O> DataResult<Short> decode(O input, DynamicOps<O> ops) {
            return ops.getShort(input);
        }
    };

    /** Codec for integer numbers. */
    Codec<Integer> INT = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Integer input, DynamicOps<O> ops) {
            return DataResult.success(ops.createInt(input));
        }

        @Override
        public <O> DataResult<Integer> decode(O input, DynamicOps<O> ops) {
            return ops.getInt(input);
        }
    };

    /** Codec for long numbers. */
    Codec<Long> LONG = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Long input, DynamicOps<O> ops) {
            return DataResult.success(ops.createLong(input));
        }

        @Override
        public <O> DataResult<Long> decode(O input, DynamicOps<O> ops) {
            return ops.getLong(input);
        }
    };

    /** Codec for float numbers. */
    Codec<Float> FLOAT = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Float input, DynamicOps<O> ops) {
            return DataResult.success(ops.createFloat(input));
        }

        @Override
        public <O> DataResult<Float> decode(O input, DynamicOps<O> ops) {
            return ops.getFloat(input);
        }
    };

    /** Codec for decimal numbers (doubles). */
    Codec<Double> DOUBLE = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Double input, DynamicOps<O> ops) {
            return DataResult.success(ops.createDouble(input));
        }

        @Override
        public <O> DataResult<Double> decode(O input, DynamicOps<O> ops) {
            return ops.getDouble(input);
        }
    };

    /** Codec for Bukkit Material items by their string name. */
    Codec<Material> MATERIAL = Codec.STRING.comapFlatMap(
            name -> {
                Material mat = Material.matchMaterial(name);
                return mat != null ? DataResult.success(mat) : DataResult.error("Unknown material: " + name);
            },
            Material::name
    );

    /** Codec for Kyori Key identifiers (e.g. "minecraft:stone"). */
    Codec<Key> KEY = Codec.STRING.xmap(Key::key, Key::asString);

    Codec<UUID> UUID = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    UUID uuid = java.util.UUID.fromString(s);
                    return DataResult.success(uuid);
                } catch (IllegalArgumentException e) {
                    return DataResult.error("Malformed UUID format: " + e.getMessage());
                }
            },
            java.util.UUID::toString);

    /**
     * Creates a codec for any Java Enum type.
     *
     * @param <E> the enum type
     * @param clazz the enum class
     * @return a codec for the specified enum
     */
    public static <E extends Enum<E>> Codec<E> enumOf(Class<E> clazz) {
        return Codec.STRING.comapFlatMap(
                name -> {
                    try {
                        return DataResult.success(Enum.valueOf(clazz, name.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        return DataResult.error("Invalid " + clazz.getSimpleName() + ": " + name);
                    }
                },
                Enum::name
        );
    }

    // --- COMBINATORS ---

    /**
     * Global fallback error message handler for the no-arg overloads of the {@code *Lenient} combinators
     */
    BiConsumer<Object, String> errorHandler = (key, error) ->
            RogueSmpCore.LOGGER.warn("Skipped invalid entry '{}': {}", key, error);


    /**
     * Creates a codec that handles lists of items.
     *
     * @param <E> the item type in the list
     * @param elementCodec the codec for single list items
     * @return a list codec
     */
    static <E> Codec<List<E>> listOf(Codec<E> elementCodec) {
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(List<E> input, DynamicOps<O> ops) {
                List<O> encodedList = new ArrayList<>();
                for (E element : input) {
                    DataResult<O> res = elementCodec.encode(element, ops);
                    if (!res.isSuccess()) return DataResult.error(res.error());
                    encodedList.add(res.result());
                }
                return DataResult.success(ops.createList(encodedList));
            }

            @Override
            public <O> DataResult<List<E>> decode(O input, DynamicOps<O> ops) {
                DataResult<List<O>> rawList = ops.getList(input);
                if (!rawList.isSuccess()) return DataResult.error(rawList.error());

                List<E> resultList = new ArrayList<>();
                List<O> rawItems = rawList.result();
                for (int i = 0; i < rawItems.size(); i++) {
                    DataResult<E> res = elementCodec.decode(rawItems.get(i), ops);
                    if (!res.isSuccess()) return DataResult.error("[" + i + "]: " + res.error());
                    resultList.add(res.result());
                }
                return DataResult.success(resultList);
            }
        };
    }

    /**
     * Like {@link #listOf(Codec)}, but a single corrupted/invalid element is skipped (reported via
     * {@code onError}) instead of failing the whole list. Encoding still goes through the strict
     * {@link #listOf(Codec)} — this is meant for tolerating bad data on read (e.g. player save files),
     * not for silently dropping data on write.
     *
     * @param <E> the item type in the list
     * @param elementCodec the codec for single list items
     * @param onError called with (index, error message) for every element that failed to decode
     * @return a list codec that tolerates individually bad elements
     */
    static <E> Codec<List<E>> lenientListOf(Codec<E> elementCodec, BiConsumer<Integer, String> onError) {
        Codec<List<E>> strict = listOf(elementCodec);
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(List<E> input, DynamicOps<O> ops) {
                return strict.encode(input, ops);
            }

            @Override
            public <O> DataResult<List<E>> decode(O input, DynamicOps<O> ops) {
                DataResult<List<O>> rawList = ops.getList(input);
                if (!rawList.isSuccess()) return DataResult.error(rawList.error());

                List<E> resultList = new ArrayList<>();
                List<O> rawItems = rawList.result();
                for (int i = 0; i < rawItems.size(); i++) {
                    DataResult<E> res = elementCodec.decode(rawItems.get(i), ops);
                    if (res.isSuccess()) {
                        resultList.add(res.result());
                    } else {
                        onError.accept(i, res.error());
                    }
                }
                return DataResult.success(resultList);
            }
        };
    }

    /**
     * Same as {@link #lenientListOf(Codec, BiConsumer)}, using the global {@link #errorHandler}
     * instead of a per-call handler.
     *
     * @param <E> the item type in the list
     * @param elementCodec the codec for single list items
     * @return a list codec that tolerates individually bad elements
     */
    static <E> Codec<List<E>> lenientListOf(Codec<E> elementCodec) {
        return lenientListOf(elementCodec, errorHandler::accept);
    }

    // --- FIELD CONVERSIONS (Codec -> MapCodec) ---

    /**
     * Attaches a key name to this standalone codec, converting it into a field-level {@link MapCodec}.
     * <p>
     * Example: Turning {@code Codec.INT} with field name {@code "age"} allows writing/reading
     * {@code "age": 30} inside an object map.
     *
     * @param name the map key name
     * @return a MapCodec targeting this field
     */
    default MapCodec<A> fieldOf(String name) {
        Codec<A> parent = this;
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(A input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> encoded = parent.encode(input, ops);
                if (!encoded.isSuccess()) return encoded;
                return DataResult.success(ops.setMapEntry(targetMap, name, encoded.result()));
            }

            @Override
            public <O> DataResult<A> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<O> field = ops.getMapField(inputMap, name);
                if (!field.isSuccess()) return DataResult.error(missingFieldMessage(name, inputMap, ops));
                DataResult<A> decoded = parent.decode(field.result(), ops);
                if (!decoded.isSuccess()) return DataResult.error("Field '" + name + "': " + decoded.error());
                return decoded;
            }
        };
    }

    /**
     * Attaches a key name to this standalone codec as an optional field returning an {@link Optional}.
     *
     * @param name the map key name
     * @return an optional field MapCodec
     */
    default MapCodec<Optional<A>> optionalFieldOf(String name) {
        Codec<A> parent = this;
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(Optional<A> input, O targetMap, DynamicOps<O> ops) {
                if (input.isPresent()) {
                    DataResult<O> res = parent.encode(input.get(), ops);
                    if (!res.isSuccess()) return res;
                    return DataResult.success(ops.setMapEntry(targetMap, name, res.result()));
                }
                return DataResult.success(targetMap);
            }

            @Override
            public <O> DataResult<Optional<A>> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<O> field = ops.getMapField(inputMap, name);
                if (!field.isSuccess()) return DataResult.success(Optional.empty());
                DataResult<A> decoded = parent.decode(field.result(), ops);
                if (!decoded.isSuccess()) return DataResult.error("Field '" + name + "': " + decoded.error());
                return decoded.map(Optional::of);
            }
        };
    }

    /**
     * Attaches a key name to this standalone codec with a static default value if the field is missing.
     *
     * @param name the map key name
     * @param defaultValue the fallback value if the field is missing
     * @return a MapCodec with a default fallback
     */
    default MapCodec<A> optionalFieldOf(String name, A defaultValue) {
        return optionalFieldOf(name, () -> defaultValue);
    }

    /**
     * Creates an optional map field with a dynamic default value supplier.
     *
     * @param name the field key name
     * @param defaultValueSupplier supplier for the default value
     * @return a map codec for the optional field
     */
    default MapCodec<A> optionalFieldOf(String name, Supplier<A> defaultValueSupplier) {
        Codec<A> parent = this;
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(A input, O targetMap, DynamicOps<O> ops) {
                A defaultVal = defaultValueSupplier.get();
                if (Objects.equals(input, defaultVal)) {
                    return DataResult.success(targetMap);
                }
                DataResult<O> res = parent.encode(input, ops);
                if (!res.isSuccess()) return res;
                return DataResult.success(ops.setMapEntry(targetMap, name, res.result()));
            }

            @Override
            public <O> DataResult<A> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<O> field = ops.getMapField(inputMap, name);
                if (!field.isSuccess()) return DataResult.success(defaultValueSupplier.get());
                DataResult<A> decoded = parent.decode(field.result(), ops);
                if (!decoded.isSuccess()) return DataResult.error("Field '" + name + "': " + decoded.error());
                return decoded;
            }
        };
    }

    /**
     * Creates an optional field that silently uses a fallback value if missing or invalid.
     *
     * @param name the field key name
     * @param fallbackValue default value to use on failure or missing field
     * @return a lenient map codec
     */
    default MapCodec<A> lenientOptionalFieldOf(String name, A fallbackValue) {
        Codec<A> parent = this;
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(A input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> res = parent.encode(input, ops);
                if (!res.isSuccess()) return res;
                return DataResult.success(ops.setMapEntry(targetMap, name, res.result()));
            }

            @Override
            public <O> DataResult<A> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<O> field = ops.getMapField(inputMap, name);
                if (!field.isSuccess()) return DataResult.success(fallbackValue);
                DataResult<A> decoded = parent.decode(field.result(), ops);
                return decoded.isSuccess() ? decoded : DataResult.success(fallbackValue);
            }
        };
    }

    // --- MAPPING METHOD VARIANTS ---

    /**
     * Converts this codec to handle a new type using two direct mapping functions.
     *
     * @param <B> the new object type
     * @param to converts A to B
     * @param from converts B back to A
     * @return a codec for type B
     */
    default <B> Codec<B> xmap(Function<A, B> to, Function<B, A> from) {
        Codec<A> parent = this;
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(B input, DynamicOps<O> ops) {
                return parent.encode(from.apply(input), ops);
            }

            @Override
            public <O> DataResult<B> decode(O input, DynamicOps<O> ops) {
                return parent.decode(input, ops).map(to);
            }
        };
    }

    /**
     * Converts this codec to handle a new type, where decoding can fail.
     *
     * @param <B> the new object type
     * @param to converts A to B (can fail)
     * @param from converts B back to A
     * @return a codec for type B
     */
    default <B> Codec<B> comapFlatMap(Function<A, DataResult<B>> to, Function<B, A> from) {
        Codec<A> parent = this;
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(B input, DynamicOps<O> ops) {
                return parent.encode(from.apply(input), ops);
            }

            @Override
            public <O> DataResult<B> decode(O input, DynamicOps<O> ops) {
                return parent.decode(input, ops).flatMap(to);
            }
        };
    }

    /**
     * Converts this codec to handle a new type, where both encoding and decoding can fail.
     *
     * @param <B> the new object type
     * @param to converts A to B (can fail)
     * @param from converts B back to A (can fail)
     * @return a codec for type B
     */
    default <B> Codec<B> flatXmap(Function<A, DataResult<B>> to, Function<B, DataResult<A>> from) {
        Codec<A> parent = this;
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(B input, DynamicOps<O> ops) {
                return from.apply(input).flatMap(a -> parent.encode(a, ops));
            }

            @Override
            public <O> DataResult<B> decode(O input, DynamicOps<O> ops) {
                return parent.decode(input, ops).flatMap(to);
            }
        };
    }

    /**
     * Tries {@code primary} first; if decoding fails, falls back to {@code alternative}. Encoding
     * always goes through {@code primary} - meant for accepting an old/shorthand input format
     * alongside a newer canonical one (e.g. a bare int shorthand alongside a full object form)
     * without every such component hand-rolling the same try/fallback dance.
     *
     * @param <A> the decoded object type
     * @param primary the canonical codec, tried first and always used for encoding
     * @param alternative the fallback codec, tried only if {@code primary} fails to decode
     * @return a codec accepting either serialized shape
     */
    static <A> Codec<A> withAlternative(Codec<A> primary, Codec<A> alternative) {
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(A input, DynamicOps<O> ops) {
                return primary.encode(input, ops);
            }

            @Override
            public <O> DataResult<A> decode(O input, DynamicOps<O> ops) {
                DataResult<A> primaryResult = primary.decode(input, ops);
                if (primaryResult.isSuccess()) return primaryResult;

                DataResult<A> alternativeResult = alternative.decode(input, ops);
                if (alternativeResult.isSuccess()) return alternativeResult;

                return DataResult.error(primaryResult.error() + " (alternative also failed: " + alternativeResult.error() + ")");
            }
        };
    }

    // --- POLYMORPHIC DISPATCH ---

    /**
     * Creates a polymorphic codec that dynamically dispatches encoding and decoding to sub-codecs
     * based on a type identifier field stored inside the serialized map object.
     *
     * @param <T> The base class or interface type (e.g. SmpEffect)
     * @param <K> The key/type identifier type (e.g. String)
     * @param typeFieldName The key name in the JSON/Data object storing the type ID (e.g., "type")
     * @param typeGetter Function to extract the type key from an object instance
     * @param keyCodec Codec used to serialize/deserialize the type key itself
     * @param codecGetter Function that resolves the subtype Codec for a given type key
     * @return A polymorphic codec handling subclass dispatch
     */
    static <T, K> Codec<T> dispatch(
            String typeFieldName,
            Function<T, K> typeGetter,
            Codec<K> keyCodec,
            Function<K, Codec<? extends T>> codecGetter
    ) {
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(T input, DynamicOps<O> ops) {
                K key = typeGetter.apply(input);
                if (key == null) {
                    return DataResult.error("Could not extract polymorphic type key from object: " + input);
                }

                Codec<T> subCodec;
                try {
                    @SuppressWarnings("unchecked")
                    Codec<T> resolved = (Codec<T>) codecGetter.apply(key);
                    subCodec = resolved;
                } catch (Exception e) {
                    return DataResult.error("No codec registered for type '" + key + "' (field '" + typeFieldName + "'): " + e.getMessage());
                }
                if (subCodec == null) {
                    return DataResult.error("No codec registered for type '" + key + "' (field '" + typeFieldName + "')");
                }

                // 1. Encode subclass fields using its specific codec
                DataResult<O> encodedObject = subCodec.encode(input, ops);
                if (!encodedObject.isSuccess()) {
                    return DataResult.error("type '" + key + "': " + encodedObject.error());
                }

                // 2. Encode the type identifier
                DataResult<O> encodedKey = keyCodec.encode(key, ops);
                if (!encodedKey.isSuccess()) {
                    return DataResult.error(encodedKey.error());
                }

                // 3. Inject the type field into the map object
                O resultMap = ops.setMapEntry(encodedObject.result(), typeFieldName, encodedKey.result());
                return DataResult.success(resultMap);
            }

            @Override
            public <O> DataResult<T> decode(O input, DynamicOps<O> ops) {
                // 1. Parse input object as a map
                DataResult<Map<String, O>> rawMap = ops.getMap(input);
                if (!rawMap.isSuccess()) {
                    return DataResult.error("Expected an object for polymorphic dispatch on field '" + typeFieldName + "', got: " + input);
                }

                Map<String, O> map = rawMap.result();
                O typeElement = map.get(typeFieldName);
                if (typeElement == null) {
                    return DataResult.error("Missing polymorphic type field '" + typeFieldName + "' (available fields: " + map.keySet() + ")");
                }

                // 2. Decode key identifier
                DataResult<K> decodedKey = keyCodec.decode(typeElement, ops);
                if (!decodedKey.isSuccess()) {
                    return DataResult.error("Failed to decode type field '" + typeFieldName + "': " + decodedKey.error());
                }

                // 3. Look up registered subclass codec
                Codec<T> subCodec;
                try {
                    @SuppressWarnings("unchecked")
                    Codec<T> resolved = (Codec<T>) codecGetter.apply(decodedKey.result());
                    subCodec = resolved;
                } catch (Exception e) {
                    return DataResult.error("No codec registered for type '" + decodedKey.result() + "' (field '" + typeFieldName + "'): " + e.getMessage());
                }
                if (subCodec == null) {
                    return DataResult.error("Unknown type '" + decodedKey.result() + "' for field '" + typeFieldName + "'");
                }

                // 4. Decode full object using specific subclass codec
                DataResult<T> decoded = subCodec.decode(input, ops);
                if (!decoded.isSuccess()) return DataResult.error("type '" + decodedKey.result() + "': " + decoded.error());
                return decoded;
            }
        };
    }

    /**
     * Convenient overload for String type keys defaulting to the name "type". See {@link #dispatch(String, Function, Codec, Function)}
     *
     * @param <T> The base class or interface type
     * @param typeGetter Function to extract the String type ID (e.g. SmpEffect::getEffectID)
     * @param codecGetter Function resolving the subclass Codec (e.g. EffectCodecRegistry::getCodec)
     */
    static <T> Codec<T> dispatch(
            Function<T, String> typeGetter,
            Function<String, Codec<? extends T>> codecGetter
    ) {
        return dispatch("type", typeGetter, Codec.STRING, codecGetter);
    }

    /**
     * Convenient overload to define String type keys as {@code typeFieldName}. See {@link #dispatch(String, Function, Codec, Function)}
     *
     * @param <T> The base class or interface type
     * @param typeFieldName  The key name in the JSON/Data object storing the type ID (e.g., "type")
     * @param typeGetter Function to extract the String type ID (e.g. SmpEffect::getEffectID)
     * @param codecGetter Function resolving the subclass Codec (e.g. EffectCodecRegistry::getCodec)
     */
    static <T> Codec<T> dispatch(
            String typeFieldName,
            Function<T, String> typeGetter,
            Function<String, Codec<? extends T>> codecGetter
    ) {
        return dispatch(typeFieldName, typeGetter, Codec.STRING, codecGetter);
    }

    /**
     * Creates a codec for maps with a custom key type K, where the value codec is dynamic
     * based on the key object (e.g. Map<Key, ItemComponent> or Map<UUID, Object>).
     * <p>
     *     <b>The key must be encoded as String!</b>
     * </p>
     *
     * @param <K> the map key type
     * @param <V> the base value type
     * @param keyCodec codec used to serialize and deserialize the key object
     * @param codecGetter function that resolves the specific codec for a given key object
     * @return a codec handling dynamically dispatched maps
     */
    static <K, V> Codec<Map<K, V>> dispatchedMap(
            Codec<K> keyCodec,
            Function<K, Codec<? extends V>> codecGetter
    ) {
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(Map<K, V> input, DynamicOps<O> ops) {
                O targetMap = ops.emptyMap();
                for (Map.Entry<K, V> entry : input.entrySet()) {
                    K key = entry.getKey();

                    // 1. Get the specific value codec for this key
                    Codec<V> valueCodec;
                    try {
                        @SuppressWarnings("unchecked")
                        Codec<V> resolved = (Codec<V>) codecGetter.apply(key);
                        valueCodec = resolved;
                    } catch (Exception e) {
                        return DataResult.error("['" + key + "']: no codec registered: " + e.getMessage());
                    }
                    if (valueCodec == null) {
                        return DataResult.error("['" + key + "']: unknown type key");
                    }

                    // 2. Turn key object K into a serialized string
                    DataResult<O> encodedKey = keyCodec.encode(key, ops);
                    if (!encodedKey.isSuccess()) return DataResult.error(encodedKey.error());

                    DataResult<String> keyString = ops.getString(encodedKey.result());
                    if (!keyString.isSuccess()) return DataResult.error("Map key did not encode as a string: " + key);

                    // 3. Encode the value
                    DataResult<O> encodedVal = valueCodec.encode(entry.getValue(), ops);
                    if (!encodedVal.isSuccess()) return DataResult.error("['" + key + "']: " + encodedVal.error());

                    targetMap = ops.setMapEntry(targetMap, keyString.result(), encodedVal.result());
                }
                return DataResult.success(targetMap);
            }

            @Override
            public <O> DataResult<Map<K, V>> decode(O input, DynamicOps<O> ops) {
                DataResult<Map<String, O>> rawMap = ops.getMap(input);
                if (!rawMap.isSuccess()) return DataResult.error(rawMap.error());

                Map<K, V> map = new HashMap<>();
                for (Map.Entry<String, O> entry : rawMap.result().entrySet()) {
                    String rawKeyString = entry.getKey();

                    // 1. Decode key string into key object K
                    DataResult<K> decodedKey = keyCodec.decode(ops.createString(rawKeyString), ops);
                    if (!decodedKey.isSuccess()) return DataResult.error("['" + rawKeyString + "']: invalid map key: " + decodedKey.error());
                    K key = decodedKey.result();

                    // 2. Get the value codec for this key object
                    Codec<V> valueCodec;
                    try {
                        @SuppressWarnings("unchecked")
                        Codec<V> resolved = (Codec<V>) codecGetter.apply(key);
                        valueCodec = resolved;
                    } catch (Exception e) {
                        return DataResult.error("['" + rawKeyString + "']: no codec registered for type '" + key + "': " + e.getMessage());
                    }
                    if (valueCodec == null) {
                        return DataResult.error("['" + rawKeyString + "']: unknown type key '" + key + "'");
                    }

                    // 3. Decode the value
                    DataResult<V> decodedVal = valueCodec.decode(entry.getValue(), ops);
                    if (!decodedVal.isSuccess()) return DataResult.error("['" + rawKeyString + "']: " + decodedVal.error());

                    map.put(key, decodedVal.result());
                }
                return DataResult.success(map);
            }
        };
    }

    /**
     * Creates a codec for maps with a String key, where the value codec is dynamic
     * based on the key name (e.g. Map<String, ItemComponent>).
     *
     * @param <V> the base value type
     * @param codecGetter function that resolves the specific codec for a given string key
     * @return a codec handling dynamically dispatched string maps
     */
    static <V> Codec<Map<String, V>> dispatchedMap(Function<String, Codec<? extends V>> codecGetter) {
        return dispatchedMap(Codec.STRING, codecGetter);
    }

    /**
     * Creates a codec for uniform maps with a custom key type K and a single fixed value codec V
     * (e.g. Map(String, Integer), Map(Key, Double), or Map(UUID, CustomData)).
     * <p>
     *     <b>The key must be encoded as String!</b>
     * </p>
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param keyCodec codec used to serialize and deserialize the key object
     * @param valueCodec fixed codec used for every value in the map
     * @return a codec handling uniform key-value maps
     */
    static <K, V> Codec<Map<K, V>> unboundedMap(Codec<K> keyCodec, Codec<V> valueCodec) {
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(Map<K, V> input, DynamicOps<O> ops) {
                O targetMap = ops.emptyMap();
                for (Map.Entry<K, V> entry : input.entrySet()) {
                    DataResult<O> keyEnc = keyCodec.encode(entry.getKey(), ops);
                    if (!keyEnc.isSuccess()) return DataResult.error(keyEnc.error());

                    DataResult<String> keyString = ops.getString(keyEnc.result());
                    if (!keyString.isSuccess()) return DataResult.error("Map key did not encode as a string: " + entry.getKey());

                    DataResult<O> valEnc = valueCodec.encode(entry.getValue(), ops);
                    if (!valEnc.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: " + valEnc.error());

                    targetMap = ops.setMapEntry(targetMap, keyString.result(), valEnc.result());
                }
                return DataResult.success(targetMap);
            }

            @Override
            public <O> DataResult<Map<K, V>> decode(O input, DynamicOps<O> ops) {
                DataResult<Map<String, O>> rawMap = ops.getMap(input);
                if (!rawMap.isSuccess()) return DataResult.error(rawMap.error());

                Map<K, V> map = new HashMap<>();
                for (Map.Entry<String, O> entry : rawMap.result().entrySet()) {
                    DataResult<K> keyDec = keyCodec.decode(ops.createString(entry.getKey()), ops);
                    if (!keyDec.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: invalid map key: " + keyDec.error());

                    DataResult<V> valDec = valueCodec.decode(entry.getValue(), ops);
                    if (!valDec.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: " + valDec.error());

                    map.put(keyDec.result(), valDec.result());
                }
                return DataResult.success(map);
            }
        };
    }

    /**
     * Creates a codec for uniform maps with String keys (e.g. Map(String, Integer)).
     *
     * @param <V> the map value type
     * @param valueCodec fixed codec used for every value in the map
     * @return a codec handling string-keyed uniform maps
     */
    static <V> Codec<Map<String, V>> unboundedMap(Codec<V> valueCodec) {
        return unboundedMap(Codec.STRING, valueCodec);
    }

    /**
     * Like {@link #unboundedMap(Codec, Codec)}, but a single corrupted/invalid entry (bad key or bad
     * value) is skipped (reported via {@code onError}) instead of failing the whole map. Encoding still
     * goes through the strict {@link #unboundedMap(Codec, Codec)} — this is meant for tolerating bad
     * data on read (e.g. player save files), not for silently dropping data on write.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param keyCodec codec used to serialize and deserialize the key object
     * @param valueCodec fixed codec used for every value in the map
     * @param onError called with (key, error message) for every entry that failed to decode; key is
     *                {@code null} if the map key itself failed to decode
     * @return a map codec that tolerates individually bad entries
     */
    static <K, V> Codec<Map<K, V>> lenientUnboundedMap(Codec<K> keyCodec, Codec<V> valueCodec, BiConsumer<K, String> onError) {
        Codec<Map<K, V>> strict = unboundedMap(keyCodec, valueCodec);
        return new Codec<>() {
            @Override
            public <O> DataResult<O> encode(Map<K, V> input, DynamicOps<O> ops) {
                return strict.encode(input, ops);
            }

            @Override
            public <O> DataResult<Map<K, V>> decode(O input, DynamicOps<O> ops) {
                DataResult<Map<String, O>> rawMap = ops.getMap(input);
                if (!rawMap.isSuccess()) return DataResult.error(rawMap.error());

                Map<K, V> map = new HashMap<>();
                for (Map.Entry<String, O> entry : rawMap.result().entrySet()) {
                    DataResult<K> keyDec = keyCodec.decode(ops.createString(entry.getKey()), ops);
                    if (!keyDec.isSuccess()) {
                        onError.accept(null, "['" + entry.getKey() + "']: invalid map key: " + keyDec.error());
                        continue;
                    }
                    K key = keyDec.result();

                    DataResult<V> valDec = valueCodec.decode(entry.getValue(), ops);
                    if (!valDec.isSuccess()) {
                        onError.accept(key, valDec.error());
                        continue;
                    }

                    map.put(key, valDec.result());
                }
                return DataResult.success(map);
            }
        };
    }

    /**
     * Like {@link #unboundedMap(Codec)}, but a single corrupted/invalid entry is skipped (reported via
     * {@code onError}) instead of failing the whole map.
     *
     * @param <V> the map value type
     * @param valueCodec fixed codec used for every value in the map
     * @param onError called with (key, error message) for every entry that failed to decode
     * @return a lenient string-keyed map codec
     */
    static <V> Codec<Map<String, V>> lenientUnboundedMap(Codec<V> valueCodec, BiConsumer<String, String> onError) {
        return lenientUnboundedMap(Codec.STRING, valueCodec, onError);
    }

    /**
     * Same as {@link #lenientUnboundedMap(Codec, Codec, BiConsumer)}, using the global
     * {@link #errorHandler} instead of a per-call handler.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param keyCodec codec used to serialize and deserialize the key object
     * @param valueCodec fixed codec used for every value in the map
     * @return a map codec that tolerates individually bad entries
     */
    static <K, V> Codec<Map<K, V>> lenientUnboundedMap(Codec<K> keyCodec, Codec<V> valueCodec) {
        return lenientUnboundedMap(keyCodec, valueCodec, errorHandler::accept);
    }

    /**
     * Same as {@link #lenientUnboundedMap(Codec, BiConsumer)}, using the global
     * {@link #errorHandler} instead of a per-call handler.
     *
     * @param <V> the map value type
     * @param valueCodec fixed codec used for every value in the map
     * @return a lenient string-keyed map codec
     */
    static <V> Codec<Map<String, V>> lenientUnboundedMap(Codec<V> valueCodec) {
        return lenientUnboundedMap(Codec.STRING, valueCodec);
    }

    /** Combines 1 field into a composite object MapCodec. */
    static <A, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            Function<A, R> factory
    ) {
        return MapCodec.composite(f1, factory);
    }

    /** Combines 2 fields into a composite object MapCodec. */
    static <A, B, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            BiFunction<A, B, R> factory
    ) {
        return MapCodec.composite(f1, f2, factory);
    }

    /** Combines 3 fields into a composite object MapCodec. */
    static <A, B, C, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.Function3<A, B, C, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, factory);
    }

    /** Combines 4 fields into a composite object MapCodec. */
    static <A, B, C, D, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.Function4<A, B, C, D, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, factory);
    }

    /** Combines 5 fields into a composite object MapCodec. */
    static <A, B, C, D, E, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.Function5<A, B, C, D, E, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, factory);
    }

    /** Combines 6 fields into a composite object MapCodec. */
    static <A, B, C, D, E, F, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.RecordField<F, R> f6,
            MapCodec.Function6<A, B, C, D, E, F, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, f6, factory);
    }
    static <A, B, C, D, E, F, G, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.RecordField<F, R> f6,
            MapCodec.RecordField<G, R> f7,
            MapCodec.Function7<A, B, C, D, E, F, G, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, f6, f7, factory);
    }

    /** Combines 8 fields into a composite object MapCodec. */
    static <A, B, C, D, E, F, G, H, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.RecordField<F, R> f6,
            MapCodec.RecordField<G, R> f7,
            MapCodec.RecordField<H, R> f8,
            MapCodec.Function8<A, B, C, D, E, F, G, H, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, f6, f7, f8, factory);
    }

    /** Combines 9 fields into a composite object MapCodec. */
    static <A, B, C, D, E, F, G, H, I, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.RecordField<F, R> f6,
            MapCodec.RecordField<G, R> f7,
            MapCodec.RecordField<H, R> f8,
            MapCodec.RecordField<I, R> f9,
            MapCodec.Function9<A, B, C, D, E, F, G, H, I, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, f6, f7, f8, f9, factory);
    }

    /** Combines 10 fields into a composite object MapCodec. */
    static <A, B, C, D, E, F, G, H, I, J, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.RecordField<F, R> f6,
            MapCodec.RecordField<G, R> f7,
            MapCodec.RecordField<H, R> f8,
            MapCodec.RecordField<I, R> f9,
            MapCodec.RecordField<J, R> f10,
            MapCodec.Function10<A, B, C, D, E, F, G, H, I, J, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, factory);
    }

    /** Combines 11 fields into a composite object MapCodec. */
    static <A, B, C, D, E, F, G, H, I, J, K, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.RecordField<F, R> f6,
            MapCodec.RecordField<G, R> f7,
            MapCodec.RecordField<H, R> f8,
            MapCodec.RecordField<I, R> f9,
            MapCodec.RecordField<J, R> f10,
            MapCodec.RecordField<K, R> f11,
            MapCodec.Function11<A, B, C, D, E, F, G, H, I, J, K, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, factory);
    }

    /** Combines 12 fields into a composite object MapCodec. */
    static <A, B, C, D, E, F, G, H, I, J, K, L, R> MapCodec<R> composite(
            MapCodec.RecordField<A, R> f1,
            MapCodec.RecordField<B, R> f2,
            MapCodec.RecordField<C, R> f3,
            MapCodec.RecordField<D, R> f4,
            MapCodec.RecordField<E, R> f5,
            MapCodec.RecordField<F, R> f6,
            MapCodec.RecordField<G, R> f7,
            MapCodec.RecordField<H, R> f8,
            MapCodec.RecordField<I, R> f9,
            MapCodec.RecordField<J, R> f10,
            MapCodec.RecordField<K, R> f11,
            MapCodec.RecordField<L, R> f12,
            MapCodec.Function12<A, B, C, D, E, F, G, H, I, J, K, L, R> factory
    ) {
        return MapCodec.composite(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, factory);
    }

}
