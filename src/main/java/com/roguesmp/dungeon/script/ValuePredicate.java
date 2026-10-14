package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public sealed interface ValuePredicate {

    // A bare value means "equals"; objects are told apart by their key (in, not, any_of, exists, range, min/max).
    Codec<ValuePredicate> CODEC = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(ValuePredicate input, DynamicOps<O> ops) {
            return switch (input) {
                case Equals(DataValue value) -> DataValue.CODEC.encode(value, ops);
                case In(List<DataValue> values) -> field(ops, "in", Codec.listOf(DataValue.CODEC).encode(values, ops));
                case Not(ValuePredicate predicate) -> field(ops, "not", ValuePredicate.CODEC.encode(predicate, ops));
                case AnyOf(List<ValuePredicate> predicates) -> field(ops, "any_of", Codec.listOf(ValuePredicate.CODEC).encode(predicates, ops));
                case Exists(boolean exists) -> field(ops, "exists", DataResult.success(ops.createBoolean(exists)));
                case Range(double min, double max) -> {
                    O map = ops.emptyMap();
                    if (min != Double.NEGATIVE_INFINITY) map = ops.setMapEntry(map, "min", ops.createDouble(min));
                    if (max != Double.POSITIVE_INFINITY) map = ops.setMapEntry(map, "max", ops.createDouble(max));
                    yield DataResult.success(map);
                }
            };
        }

        @Override
        public <O> DataResult<ValuePredicate> decode(O input, DynamicOps<O> ops) {
            DataResult<Map<String, O>> map = ops.getMap(input);
            if (!map.isSuccess()) return DataValue.CODEC.decode(input, ops).map(Equals::new);

            Map<String, O> fields = map.result();
            if (fields.containsKey("in")) return Codec.listOf(DataValue.CODEC).decode(fields.get("in"), ops).map(In::new);
            if (fields.containsKey("not")) return ValuePredicate.CODEC.decode(fields.get("not"), ops).map(Not::new);
            if (fields.containsKey("any_of")) return Codec.listOf(ValuePredicate.CODEC).decode(fields.get("any_of"), ops).map(AnyOf::new);
            if (fields.containsKey("exists")) return Codec.BOOLEAN.decode(fields.get("exists"), ops).map(Exists::new);
            if (fields.containsKey("range")) {
                return Codec.listOf(Codec.DOUBLE).decode(fields.get("range"), ops).flatMap(bounds -> bounds.size() == 2
                        ? DataResult.success(new Range(bounds.get(0), bounds.get(1)))
                        : DataResult.error("range needs [min, max]"));
            }
            if (fields.containsKey("min") || fields.containsKey("max")) {
                DataResult<Double> min = fields.containsKey("min") ? Codec.DOUBLE.decode(fields.get("min"), ops) : DataResult.success(Double.NEGATIVE_INFINITY);
                DataResult<Double> max = fields.containsKey("max") ? Codec.DOUBLE.decode(fields.get("max"), ops) : DataResult.success(Double.POSITIVE_INFINITY);
                return min.flatMap(low -> max.map(high -> new Range(low, high)));
            }
            return DataResult.error("Unknown predicate " + fields.keySet());
        }

        private static <O> DataResult<O> field(DynamicOps<O> ops, String key, DataResult<O> value) {
            return value.map(encoded -> ops.setMapEntry(ops.emptyMap(), key, encoded));
        }
    };

    boolean test(@Nullable DataValue value);

    record Equals(DataValue value) implements ValuePredicate {
        @Override
        public boolean test(@Nullable DataValue value) {
            return this.value.equals(value);
        }
    }

    record In(List<DataValue> values) implements ValuePredicate {
        @Override
        public boolean test(@Nullable DataValue value) {
            return values.contains(value);
        }
    }

    record Range(double min, double max) implements ValuePredicate {
        @Override
        public boolean test(@Nullable DataValue value) {
            return value instanceof DataValue.NumberValue(double number) && number >= min && number <= max;
        }
    }

    record Not(ValuePredicate predicate) implements ValuePredicate {
        @Override
        public boolean test(@Nullable DataValue value) {
            return !predicate.test(value);
        }
    }

    record AnyOf(List<ValuePredicate> predicates) implements ValuePredicate {
        @Override
        public boolean test(@Nullable DataValue value) {
            return predicates.stream().anyMatch(predicate -> predicate.test(value));
        }
    }

    record Exists(boolean exists) implements ValuePredicate {
        @Override
        public boolean test(@Nullable DataValue value) {
            return (value != null) == exists;
        }
    }
}
