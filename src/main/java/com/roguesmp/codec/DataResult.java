package com.roguesmp.codec;

import java.util.function.Function;

/**
 * Holds either a successful output value or an error message.
 *
 * @param <R> the type of the result
 * @param result the value if successful, or null if there was an error
 * @param error the error message if failed, or null if successful
 */
public record DataResult<R>(R result, String error) {

    /**
     * Creates a successful result containing a value.
     *
     * @param <R> the result type
     * @param result the output value to store
     * @return a successful DataResult
     */
    public static <R> DataResult<R> success(R result) {
        return new DataResult<>(result, null);
    }

    /**
     * Creates a failed result containing an error message.
     *
     * @param <R> the result type
     * @param error the error message explaining what went wrong
     * @return a failed DataResult
     */
    public static <R> DataResult<R> error(String error) {
        return new DataResult<>(null, error);
    }

    /**
     * Checks if the operation succeeded.
     *
     * @return true if successful, false if there is an error
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Converts the result value into something else if successful.
     * If this is an error, passes the error along without changing it.
     *
     * @param <T> the new result type
     * @param mapper the function to change the value
     * @return a new DataResult with the converted value, or the original error
     */
    public <T> DataResult<T> map(Function<R, T> mapper) {
        return isSuccess() ? success(mapper.apply(result)) : error(error);
    }

    /**
     * Converts the result value using a function that returns another DataResult.
     * If this is an error, passes the error along without changing it.
     *
     * @param <T> the new result type
     * @param mapper the function that produces a new DataResult
     * @return the new DataResult, or the original error
     */
    public <T> DataResult<T> flatMap(Function<R, DataResult<T>> mapper) {
        return isSuccess() ? mapper.apply(result) : error(error);
    }
}
