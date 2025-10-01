package com.recplatform.data.query;

import com.recplatform.data.query.dto.QueryRequest;
import com.recplatform.data.query.dto.QueryResult;

/**
 * OLAP query service interface.
 */
public interface QueryService {

    QueryResult executeQuery(QueryRequest request);

    QueryResult preview(QueryRequest request);
}
