package com.recplatform.data.pipeline;

/**
 * Pipeline node sub-type enumeration.
 */
public enum NodeSubType {
    DB_QUERY,
    API_CALL,
    FILE_READ,
    FILTER,
    MAP,
    JOIN,
    AGGREGATE,
    SCRIPT,
    DB_WRITE,
    API_PUSH,
    FILE_EXPORT
}
