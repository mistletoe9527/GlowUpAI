package com.glowupai.common;

import java.util.List;

/**
 * 分页响应数据。
 *
 * @param records 当前页数据
 * @param total 总记录数
 * @param page 当前页码
 * @param pageSize 每页条数
 * @param <T> 分页元素类型
 */
public record PageResult<T>(
        List<T> records,
        long total,
        int page,
        int pageSize
) {
}
