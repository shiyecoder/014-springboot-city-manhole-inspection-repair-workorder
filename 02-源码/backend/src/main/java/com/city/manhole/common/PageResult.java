package com.city.manhole.common;

import lombok.Data;

import java.util.List;

/**
 * 服务端分页返回：总数、页码、每页条数、数据列表
 */
@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long size;
    private List<T> records;

    public PageResult(long total, long page, long size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }
}
