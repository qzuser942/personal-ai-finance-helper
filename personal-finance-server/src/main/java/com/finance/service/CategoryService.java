package com.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.entity.Category;

import java.util.List;

/**
 * 分类 Service
 */
public interface CategoryService extends IService<Category> {

    /** 获取用户可用分类列表 */
    List<Category> getUserCategories(Long userId, String type);

    /** 创建自定义分类 */
    Category addCustomCategory(Category category, Long userId);

    /** 修改分类 */
    Category updateCategory(Long categoryId, Category category, Long userId, boolean isAdmin);

    /** 删除分类 */
    void deleteCategory(Long categoryId, Long userId, boolean isAdmin);

    /** 初始化系统默认分类 */
    void initSystemCategories();
}
