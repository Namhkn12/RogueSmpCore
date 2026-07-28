package com.roguesmp.codec;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A specialized {@link Codec} that operates directly on key-value fields inside a map object,
 * rather than reading/writing a standalone value.
 * <p>
 * <b>Codec vs MapCodec:</b>
 * <ul>
 *   <li>A standard {@code Codec<A>} converts a standalone value (e.g., {@code 42}, {@code "hello"}, or a raw list).</li>
 *   <li>A {@code MapCodec<A>} reads or writes key-value entries directly inside an existing map (e.g., {@code "age": 42}).</li>
 * </ul>
 * <p>
 * Because a {@code MapCodec} works on fields within a map, multiple {@code MapCodec}s can be combined
 * flatly into a single parent object using the {@code composite(...)} methods.
 *
 * @param <A> the Java object type this map codec handles
 */
public abstract class MapCodec<A> implements Codec<A> {

    /**
     * Encodes an object's fields directly into an existing map.
     * <p>
     * Unlike {@link #encode}, this appends key-value pairs directly to {@code targetMap}
     * instead of creating a new standalone value object.
     *
     * @param <O> the target data format type
     * @param input the object to encode
     * @param targetMap the map object to write fields into
     * @param ops the format helper
     * @return a result with the updated map, or an error
     */
    public abstract <O> DataResult<O> encodeFields(A input, O targetMap, DynamicOps<O> ops);

    /**
     * Reads an object's fields directly from a map object.
     *
     * @param <O> the input data format type
     * @param inputMap the source map object containing the fields
     * @param ops the format helper
     * @return a result with the decoded object, or an error
     */
    public abstract <O> DataResult<A> decodeFields(O inputMap, DynamicOps<O> ops);

    // --- IMPLEMENTING CODEC DIRECTLY ---

    /**
     * Encodes this object by creating a new, empty map and writing its fields into it.
     */
    @Override
    public <O> DataResult<O> encode(A input, DynamicOps<O> ops) {
        return encodeFields(input, ops.emptyMap(), ops);
    }

    /**
     * Decodes this object directly from a map.
     */
    @Override
    public <O> DataResult<A> decode(O input, DynamicOps<O> ops) {
        return decodeFields(input, ops);
    }

    /**
     * Returns this MapCodec as a standard {@link Codec}.
     *
     * @return this instance
     */
    public Codec<A> codec() {
        return this;
    }

    /**
     * A MapCodec with no fields of its own — decode always succeeds by calling {@code instance},
     * ignoring whatever else is in the object; encode contributes nothing. Useful for a dispatch
     * case (see {@link Codec#dispatch}) that only needs its type key and no other data, e.g. a
     * "no configurable params" spell/effect/requirement variant.
     *
     * @param <A> the type produced
     * @param instance supplies the (typically stateless/singleton-shaped) value to decode to
     * @return a MapCodec that reads/writes no fields
     */
    public static <A> MapCodec<A> unit(Supplier<A> instance) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(A input, O targetMap, DynamicOps<O> ops) {
                return DataResult.success(targetMap);
            }

            @Override
            public <O> DataResult<A> decodeFields(O inputMap, DynamicOps<O> ops) {
                return DataResult.success(instance.get());
            }
        };
    }

    // --- BINDING AND COMPOSITES ---

    /**
     * Pairs a field's {@link MapCodec} with a getter function so it can be extracted from a parent object.
     *
     * @param <A> the field type
     * @param <R> the parent object type
     * @param codec the map codec handling this field
     * @param getter function to get this field's value from the parent object
     */
    public record RecordField<A, R>(MapCodec<A> codec, Function<R, A> getter) {}

    /**
     * Binds a getter function to this MapCodec so it can be used inside a {@code composite(...)} object builder.
     *
     * @param <R> the parent object type
     * @param getter function to extract this field from the parent object
     * @return a RecordField linking this codec to the getter
     */
    public <R> RecordField<A, R> forGetter(Function<R, A> getter) {
        return new RecordField<>(this, getter);
    }

    // --- MAPCODEC MAPPINGS ---

    /**
     * Converts this MapCodec to handle a new object type using two direct mapping functions.
     */
    @Override
    public <B> MapCodec<B> xmap(Function<A, B> to, Function<B, A> from) {
        MapCodec<A> parent = this;
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(B input, O targetMap, DynamicOps<O> ops) {
                return parent.encodeFields(from.apply(input), targetMap, ops);
            }

            @Override
            public <O> DataResult<B> decodeFields(O inputMap, DynamicOps<O> ops) {
                return parent.decodeFields(inputMap, ops).map(to);
            }
        };
    }

    /**
     * Converts this MapCodec to handle a new object type, where decoding can fail.
     */
    @Override
    public <B> MapCodec<B> comapFlatMap(Function<A, DataResult<B>> to, Function<B, A> from) {
        MapCodec<A> parent = this;
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(B input, O targetMap, DynamicOps<O> ops) {
                return parent.encodeFields(from.apply(input), targetMap, ops);
            }

            @Override
            public <O> DataResult<B> decodeFields(O inputMap, DynamicOps<O> ops) {
                return parent.decodeFields(inputMap, ops).flatMap(to);
            }
        };
    }

    @Override
    public <B> MapCodec<B> flatXmap(Function<A, DataResult<B>> to, Function<B, DataResult<A>> from) {
        MapCodec<A> parent = this;
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(B input, O targetMap, DynamicOps<O> ops) {
                return from.apply(input).flatMap(a -> parent.encodeFields(a, targetMap, ops));
            }

            @Override
            public <O> DataResult<B> decodeFields(O inputMap, DynamicOps<O> ops) {
                return parent.decodeFields(inputMap, ops).flatMap(to);
            }
        };
    }

    // --- FUNCTIONAL INTERFACES FOR FACTORIES ---

    /** A function that takes 3 inputs and returns a result. */
    @FunctionalInterface
    public interface Function3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    /** A function that takes 4 inputs and returns a result. */
    @FunctionalInterface
    public interface Function4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    /** A function that takes 5 inputs and returns a result. */
    @FunctionalInterface
    public interface Function5<A, B, C, D, E, R> {
        R apply(A a, B b, C c, D d, E e);
    }

    /** A function that takes 6 inputs and returns a result. */
    @FunctionalInterface
    public interface Function6<A, B, C, D, E, F, R> {
        R apply(A a, B b, C c, D d, E e, F f);
    }

    @FunctionalInterface
    public interface Function7<A, B, C, D, E, F, G, R> {
        R apply(A a, B b, C c, D d, E e, F f, G g);
    }

    @FunctionalInterface
    public interface Function8<A, B, C, D, E, F, G, H, R> {
        R apply(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    @FunctionalInterface
    public interface Function9<A, B, C, D, E, F, G, H, I, R> {
        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i);
    }

    @FunctionalInterface
    public interface Function10<A, B, C, D, E, F, G, H, I, J, R> {
        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
    }

    @FunctionalInterface
    public interface Function11<A, B, C, D, E, F, G, H, I, J, K, R> {
        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k);
    }

    @FunctionalInterface
    public interface Function12<A, B, C, D, E, F, G, H, I, J, K, L, R> {
        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l);
    }

    // --- COMPOSITE METHODS ---

    /**
     * Combines 1 field into a composite object MapCodec.
     */
    public static <A, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            Function<A, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                return f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                return DataResult.success(factory.apply(a.result()));
            }
        };
    }

    /**
     * Combines 2 fields into a composite object MapCodec.
     */
    public static <A, B, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            BiFunction<A, B, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                return f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                return DataResult.success(factory.apply(a.result(), b.result()));
            }
        };
    }

    /**
     * Combines 3 fields into a composite object MapCodec.
     */
    public static <A, B, C, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            Function3<A, B, C, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                return f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result()));
            }
        };
    }

    /**
     * Combines 4 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            Function4<A, B, C, D, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                return f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result()));
            }
        };
    }

    /**
     * Combines 5 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            Function5<A, B, C, D, E, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                return f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result(), e.result()));
            }
        };
    }

    /**
     * Combines 6 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, F, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            RecordField<F, R> f6,
            Function6<A, B, C, D, E, F, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                DataResult<O> m5 = f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
                if (!m5.isSuccess()) return m5;
                return f6.codec().encodeFields(f6.getter().apply(input), m5.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                DataResult<F> f = f6.codec().decodeFields(inputMap, ops);
                if (!f.isSuccess()) return DataResult.error(f.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result(), e.result(), f.result()));
            }
        };
    }

    /**
     * Combines 7 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, F, G, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            RecordField<F, R> f6,
            RecordField<G, R> f7,
            Function7<A, B, C, D, E, F, G, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                DataResult<O> m5 = f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
                if (!m5.isSuccess()) return m5;
                DataResult<O> m6 = f6.codec().encodeFields(f6.getter().apply(input), m5.result(), ops);
                if (!m6.isSuccess()) return m6;
                return f7.codec().encodeFields(f7.getter().apply(input), m6.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                DataResult<F> f = f6.codec().decodeFields(inputMap, ops);
                if (!f.isSuccess()) return DataResult.error(f.error());
                DataResult<G> g = f7.codec().decodeFields(inputMap, ops);
                if (!g.isSuccess()) return DataResult.error(g.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result(), e.result(), f.result(), g.result()));
            }
        };
    }

    /**
     * Combines 8 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, F, G, H, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            RecordField<F, R> f6,
            RecordField<G, R> f7,
            RecordField<H, R> f8,
            Function8<A, B, C, D, E, F, G, H, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                DataResult<O> m5 = f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
                if (!m5.isSuccess()) return m5;
                DataResult<O> m6 = f6.codec().encodeFields(f6.getter().apply(input), m5.result(), ops);
                if (!m6.isSuccess()) return m6;
                DataResult<O> m7 = f7.codec().encodeFields(f7.getter().apply(input), m6.result(), ops);
                if (!m7.isSuccess()) return m7;
                return f8.codec().encodeFields(f8.getter().apply(input), m7.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                DataResult<F> f = f6.codec().decodeFields(inputMap, ops);
                if (!f.isSuccess()) return DataResult.error(f.error());
                DataResult<G> g = f7.codec().decodeFields(inputMap, ops);
                if (!g.isSuccess()) return DataResult.error(g.error());
                DataResult<H> h = f8.codec().decodeFields(inputMap, ops);
                if (!h.isSuccess()) return DataResult.error(h.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result(), e.result(), f.result(), g.result(), h.result()));
            }
        };
    }

    /**
     * Combines 9 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, F, G, H, I, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            RecordField<F, R> f6,
            RecordField<G, R> f7,
            RecordField<H, R> f8,
            RecordField<I, R> f9,
            Function9<A, B, C, D, E, F, G, H, I, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                DataResult<O> m5 = f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
                if (!m5.isSuccess()) return m5;
                DataResult<O> m6 = f6.codec().encodeFields(f6.getter().apply(input), m5.result(), ops);
                if (!m6.isSuccess()) return m6;
                DataResult<O> m7 = f7.codec().encodeFields(f7.getter().apply(input), m6.result(), ops);
                if (!m7.isSuccess()) return m7;
                DataResult<O> m8 = f8.codec().encodeFields(f8.getter().apply(input), m7.result(), ops);
                if (!m8.isSuccess()) return m8;
                return f9.codec().encodeFields(f9.getter().apply(input), m8.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                DataResult<F> f = f6.codec().decodeFields(inputMap, ops);
                if (!f.isSuccess()) return DataResult.error(f.error());
                DataResult<G> g = f7.codec().decodeFields(inputMap, ops);
                if (!g.isSuccess()) return DataResult.error(g.error());
                DataResult<H> h = f8.codec().decodeFields(inputMap, ops);
                if (!h.isSuccess()) return DataResult.error(h.error());
                DataResult<I> i = f9.codec().decodeFields(inputMap, ops);
                if (!i.isSuccess()) return DataResult.error(i.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result(), e.result(), f.result(), g.result(), h.result(), i.result()));
            }
        };
    }

    /**
     * Combines 10 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, F, G, H, I, J, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            RecordField<F, R> f6,
            RecordField<G, R> f7,
            RecordField<H, R> f8,
            RecordField<I, R> f9,
            RecordField<J, R> f10,
            Function10<A, B, C, D, E, F, G, H, I, J, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                DataResult<O> m5 = f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
                if (!m5.isSuccess()) return m5;
                DataResult<O> m6 = f6.codec().encodeFields(f6.getter().apply(input), m5.result(), ops);
                if (!m6.isSuccess()) return m6;
                DataResult<O> m7 = f7.codec().encodeFields(f7.getter().apply(input), m6.result(), ops);
                if (!m7.isSuccess()) return m7;
                DataResult<O> m8 = f8.codec().encodeFields(f8.getter().apply(input), m7.result(), ops);
                if (!m8.isSuccess()) return m8;
                DataResult<O> m9 = f9.codec().encodeFields(f9.getter().apply(input), m8.result(), ops);
                if (!m9.isSuccess()) return m9;
                return f10.codec().encodeFields(f10.getter().apply(input), m9.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                DataResult<F> f = f6.codec().decodeFields(inputMap, ops);
                if (!f.isSuccess()) return DataResult.error(f.error());
                DataResult<G> g = f7.codec().decodeFields(inputMap, ops);
                if (!g.isSuccess()) return DataResult.error(g.error());
                DataResult<H> h = f8.codec().decodeFields(inputMap, ops);
                if (!h.isSuccess()) return DataResult.error(h.error());
                DataResult<I> i = f9.codec().decodeFields(inputMap, ops);
                if (!i.isSuccess()) return DataResult.error(i.error());
                DataResult<J> j = f10.codec().decodeFields(inputMap, ops);
                if (!j.isSuccess()) return DataResult.error(j.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result(), e.result(), f.result(), g.result(), h.result(), i.result(), j.result()));
            }
        };
    }

    /**
     * Combines 11 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, F, G, H, I, J, K, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            RecordField<F, R> f6,
            RecordField<G, R> f7,
            RecordField<H, R> f8,
            RecordField<I, R> f9,
            RecordField<J, R> f10,
            RecordField<K, R> f11,
            Function11<A, B, C, D, E, F, G, H, I, J, K, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                DataResult<O> m5 = f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
                if (!m5.isSuccess()) return m5;
                DataResult<O> m6 = f6.codec().encodeFields(f6.getter().apply(input), m5.result(), ops);
                if (!m6.isSuccess()) return m6;
                DataResult<O> m7 = f7.codec().encodeFields(f7.getter().apply(input), m6.result(), ops);
                if (!m7.isSuccess()) return m7;
                DataResult<O> m8 = f8.codec().encodeFields(f8.getter().apply(input), m7.result(), ops);
                if (!m8.isSuccess()) return m8;
                DataResult<O> m9 = f9.codec().encodeFields(f9.getter().apply(input), m8.result(), ops);
                if (!m9.isSuccess()) return m9;
                DataResult<O> m10 = f10.codec().encodeFields(f10.getter().apply(input), m9.result(), ops);
                if (!m10.isSuccess()) return m10;
                return f11.codec().encodeFields(f11.getter().apply(input), m10.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                DataResult<F> f = f6.codec().decodeFields(inputMap, ops);
                if (!f.isSuccess()) return DataResult.error(f.error());
                DataResult<G> g = f7.codec().decodeFields(inputMap, ops);
                if (!g.isSuccess()) return DataResult.error(g.error());
                DataResult<H> h = f8.codec().decodeFields(inputMap, ops);
                if (!h.isSuccess()) return DataResult.error(h.error());
                DataResult<I> i = f9.codec().decodeFields(inputMap, ops);
                if (!i.isSuccess()) return DataResult.error(i.error());
                DataResult<J> j = f10.codec().decodeFields(inputMap, ops);
                if (!j.isSuccess()) return DataResult.error(j.error());
                DataResult<K> k = f11.codec().decodeFields(inputMap, ops);
                if (!k.isSuccess()) return DataResult.error(k.error());
                return DataResult.success(factory.apply(a.result(), b.result(), c.result(), d.result(), e.result(), f.result(), g.result(), h.result(), i.result(), j.result(), k.result()));
            }
        };
    }

    /**
     * Combines 12 fields into a composite object MapCodec.
     */
    public static <A, B, C, D, E, F, G, H, I, J, K, L, R> MapCodec<R> composite(
            RecordField<A, R> f1,
            RecordField<B, R> f2,
            RecordField<C, R> f3,
            RecordField<D, R> f4,
            RecordField<E, R> f5,
            RecordField<F, R> f6,
            RecordField<G, R> f7,
            RecordField<H, R> f8,
            RecordField<I, R> f9,
            RecordField<J, R> f10,
            RecordField<K, R> f11,
            RecordField<L, R> f12,
            Function12<A, B, C, D, E, F, G, H, I, J, K, L, R> factory
    ) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<O> encodeFields(R input, O targetMap, DynamicOps<O> ops) {
                DataResult<O> m1 = f1.codec().encodeFields(f1.getter().apply(input), targetMap, ops);
                if (!m1.isSuccess()) return m1;
                DataResult<O> m2 = f2.codec().encodeFields(f2.getter().apply(input), m1.result(), ops);
                if (!m2.isSuccess()) return m2;
                DataResult<O> m3 = f3.codec().encodeFields(f3.getter().apply(input), m2.result(), ops);
                if (!m3.isSuccess()) return m3;
                DataResult<O> m4 = f4.codec().encodeFields(f4.getter().apply(input), m3.result(), ops);
                if (!m4.isSuccess()) return m4;
                DataResult<O> m5 = f5.codec().encodeFields(f5.getter().apply(input), m4.result(), ops);
                if (!m5.isSuccess()) return m5;
                DataResult<O> m6 = f6.codec().encodeFields(f6.getter().apply(input), m5.result(), ops);
                if (!m6.isSuccess()) return m6;
                DataResult<O> m7 = f7.codec().encodeFields(f7.getter().apply(input), m6.result(), ops);
                if (!m7.isSuccess()) return m7;
                DataResult<O> m8 = f8.codec().encodeFields(f8.getter().apply(input), m7.result(), ops);
                if (!m8.isSuccess()) return m8;
                DataResult<O> m9 = f9.codec().encodeFields(f9.getter().apply(input), m8.result(), ops);
                if (!m9.isSuccess()) return m9;
                DataResult<O> m10 = f10.codec().encodeFields(f10.getter().apply(input), m9.result(), ops);
                if (!m10.isSuccess()) return m10;
                DataResult<O> m11 = f11.codec().encodeFields(f11.getter().apply(input), m10.result(), ops);
                if (!m11.isSuccess()) return m11;
                return f12.codec().encodeFields(f12.getter().apply(input), m11.result(), ops);
            }

            @Override
            public <O> DataResult<R> decodeFields(O inputMap, DynamicOps<O> ops) {
                DataResult<A> a = f1.codec().decodeFields(inputMap, ops);
                if (!a.isSuccess()) return DataResult.error(a.error());
                DataResult<B> b = f2.codec().decodeFields(inputMap, ops);
                if (!b.isSuccess()) return DataResult.error(b.error());
                DataResult<C> c = f3.codec().decodeFields(inputMap, ops);
                if (!c.isSuccess()) return DataResult.error(c.error());
                DataResult<D> d = f4.codec().decodeFields(inputMap, ops);
                if (!d.isSuccess()) return DataResult.error(d.error());
                DataResult<E> e = f5.codec().decodeFields(inputMap, ops);
                if (!e.isSuccess()) return DataResult.error(e.error());
                DataResult<F> f = f6.codec().decodeFields(inputMap, ops);
                if (!f.isSuccess()) return DataResult.error(f.error());
                DataResult<G> g = f7.codec().decodeFields(inputMap, ops);
                if (!g.isSuccess()) return DataResult.error(g.error());
                DataResult<H> h = f8.codec().decodeFields(inputMap, ops);
                if (!h.isSuccess()) return DataResult.error(h.error());
                DataResult<I> i = f9.codec().decodeFields(inputMap, ops);
                if (!i.isSuccess()) return DataResult.error(i.error());
                DataResult<J> j = f10.codec().decodeFields(inputMap, ops);
                if (!j.isSuccess()) return DataResult.error(j.error());
                DataResult<K> k = f11.codec().decodeFields(inputMap, ops);
                if (!k.isSuccess()) return DataResult.error(k.error());
                DataResult<L> l = f12.codec().decodeFields(inputMap, ops);
                if (!l.isSuccess()) return DataResult.error(l.error());
                return DataResult.success(factory.apply(
                        a.result(), b.result(), c.result(), d.result(), e.result(), f.result(),
                        g.result(), h.result(), i.result(), j.result(), k.result(), l.result()
                ));
            }
        };
    }
}
