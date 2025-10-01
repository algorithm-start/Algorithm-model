package com.recplatform.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Generic page result wrapper.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;

    private long total;

    private long page;

    private long size;

    private long pages;

    /**
     * Build a PageResult from a MyBatis-Plus IPage.
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return PageResult.<T>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .page(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .build();
    }

    /**
     * Build an empty PageResult.
     */
    public static <T> PageResult<T> empty(long page, long size) {
        return PageResult.<T>builder()
                .records(Collections.emptyList())
                .total(0)
                .page(page)
                .size(size)
                .pages(0)
                .build();
    }
}
