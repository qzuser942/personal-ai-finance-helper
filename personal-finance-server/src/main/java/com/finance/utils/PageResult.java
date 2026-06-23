package com.finance.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页返回体
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 数据列表 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    /** 总页数 */
    private Integer totalPages;

    /**
     * 从MyBatis-Plus分页结果转换
     */
    public static <T> PageResult<T> from(IPage<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                (int) page.getPages()
        );
    }

    /**
     * 手动构建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Integer page, Integer size) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(records, total, page, size, totalPages);
    }
}
