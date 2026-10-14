package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;

public sealed interface DataValue {

    Codec<DataValue> CODEC = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(DataValue input, DynamicOps<O> ops) {
            return DataResult.success(switch (input) {
                case StringValue(String value) -> ops.createString(value);
                case BooleanValue(boolean value) -> ops.createBoolean(value);
                case NumberValue number when number.isWhole() -> ops.createLong((long) number.value());
                case NumberValue(double value) -> ops.createDouble(value);
            });
        }

        @Override
        public <O> DataResult<DataValue> decode(O input, DynamicOps<O> ops) {
            DataResult<Boolean> bool = ops.getBoolean(input);
            if (bool.isSuccess()) return DataResult.success(of(bool.result()));
            DataResult<Double> number = ops.getDouble(input);
            if (number.isSuccess()) return DataResult.success(of(number.result()));
            DataResult<String> string = ops.getString(input);
            if (string.isSuccess()) return DataResult.success(of(string.result()));
            return DataResult.error("Expected a string, number or boolean: " + input);
        }
    };

    static DataValue of(String value) {
        return new StringValue(value);
    }

    static DataValue of(double value) {
        return new NumberValue(value);
    }

    static DataValue of(boolean value) {
        return new BooleanValue(value);
    }

    String display();

    record StringValue(String value) implements DataValue {
        @Override
        public String display() {
            return value;
        }
    }

    record NumberValue(double value) implements DataValue {
        boolean isWhole() {
            return value == Math.rint(value) && !Double.isInfinite(value);
        }

        @Override
        public String display() {
            return isWhole() ? String.valueOf((long) value) : String.valueOf(value);
        }
    }

    record BooleanValue(boolean value) implements DataValue {
        @Override
        public String display() {
            return String.valueOf(value);
        }
    }
}
