package com.gss.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果包装
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private Long total;

    /** 当前页数据 */
    private List<T> rows;

    /**
     * 快速构建分页结果
     */
    public static <T> PageResult<T> of(Long total, List<T> rows) {
        return new PageResult<>(total, rows);
    }
}
