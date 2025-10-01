package com.recplatform.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * JSON utility wrapper around Jackson ObjectMapper.
 */
@Slf4j
public class JsonUtil {

    private static final ObjectMapper MAPPER = createMapper();

    private JsonUtil() {
    }

    /**
     * Get the shared ObjectMapper instance.
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Serialize an object to JSON string.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", obj.getClass().getName(), e);
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Serialize an object to pretty-printed JSON string.
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to pretty JSON: {}", obj.getClass().getName(), e);
            throw new RuntimeException("Failed to serialize object to pretty JSON", e);
        }
    }

    /**
     * Deserialize a JSON string to an object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}: {}", clazz.getName(), json, e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    /**
     * Deserialize a JSON string to a parameterized type.
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}", typeRef.getType(), e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    /**
     * Deserialize from an InputStream.
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }
        try {
            return MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("Failed to deserialize InputStream to {}", clazz.getName(), e);
            throw new RuntimeException("Failed to deserialize JSON from InputStream", e);
        }
    }

    /**
     * Convert an object to another type using JSON as intermediate representation.
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return MAPPER.convertValue(fromValue, toValueType);
    }

    /**
     * Convert an object to another type using JSON as intermediate representation.
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        return MAPPER.convertValue(fromValue, toValueTypeRef);
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());
        // Ignore unknown properties during deserialization
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Do not serialize dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Include non-null only
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Date format
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return mapper;
    }
}
