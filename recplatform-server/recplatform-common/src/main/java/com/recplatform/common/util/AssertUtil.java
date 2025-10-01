package com.recplatform.common.util;

import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.ResultCode;

import java.util.Collection;
import java.util.Map;

/**
 * Assertion helpers that throw BusinessException on failure.
 */
public class AssertUtil {

    private AssertUtil() {
    }

    /**
     * Assert that an object is not null.
     */
    public static void notNull(Object object, ResultCode resultCode) {
        if (object == null) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * Assert that an object is not null with a custom message.
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }

    /**
     * Assert that a string is not empty.
     */
    public static void notEmpty(String text, ResultCode resultCode) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * Assert that a string is not empty with a custom message.
     */
    public static void notEmpty(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }

    /**
     * Assert that a collection is not empty.
     */
    public static void notEmpty(Collection<?> collection, ResultCode resultCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * Assert that a collection is not empty with a custom message.
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }

    /**
     * Assert that a map is not empty.
     */
    public static void notEmpty(Map<?, ?> map, ResultCode resultCode) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * Assert that a map is not empty with a custom message.
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }

    /**
     * Assert that a boolean expression is true.
     */
    public static void isTrue(boolean expression, ResultCode resultCode) {
        if (!expression) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * Assert that a boolean expression is true with a custom message.
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }

    /**
     * Assert that the state matches the expected condition.
     */
    public static void state(boolean expression, ResultCode resultCode) {
        if (!expression) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * Assert that the state matches the expected condition with a custom message.
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, message);
        }
    }
}
