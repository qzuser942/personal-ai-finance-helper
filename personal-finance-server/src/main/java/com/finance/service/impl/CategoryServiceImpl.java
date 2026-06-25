package com.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.Category;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.CategoryMapper;
import com.finance.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> getUserCategories(Long userId, String type) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        // user_id = 0（系统）OR user_id = 当前用户
        wrapper.and(w -> w.eq(Category::getUserId, 0L).or().eq(Category::getUserId, userId));
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Category::getType, type);
        }
        wrapper.orderByAsc(Category::getSortOrder).orderByAsc(Category::getId);
        return list(wrapper);
    }

    @Override
    @Transactional
    public Category addCustomCategory(Category category, Long userId) {
        category.setUserId(userId);
        if (category.getSortOrder() == null) category.setSortOrder(0);

        // 检查同名同类型分类是否已存在
        LambdaQueryWrapper<Category> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Category::getName, category.getName())
                .eq(Category::getType, category.getType())
                .and(w -> w.eq(Category::getUserId, 0L).or().eq(Category::getUserId, userId));
        if (count(checkWrapper) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        save(category);
        log.info("用户{}创建自定义分类: {}", userId, category.getName());
        return category;
    }

    @Override
    @Transactional
    public Category updateCategory(Long categoryId, Category update, Long userId, boolean isAdmin) {
        Category category = getById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_AVAILABLE);
        }
        // 系统分类保护
        if (!isAdmin && category.getUserId() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_CATEGORY_PROTECTED);
        }
        // 用户只能改自己的分类
        if (!isAdmin && !category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_AVAILABLE);
        }
        if (update.getName() != null) category.setName(update.getName());
        if (update.getIcon() != null) category.setIcon(update.getIcon());
        if (update.getSortOrder() != null) category.setSortOrder(update.getSortOrder());
        updateById(category);
        return category;
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, Long userId, boolean isAdmin) {
        Category category = getById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_AVAILABLE);
        }
        // 系统分类保护 - 关键修复：超管可删除，非超管不可
        if (category.getUserId() == 0 && !isAdmin) {
            throw new BusinessException(ErrorCode.SYSTEM_CATEGORY_PROTECTED);
        }
        if (!isAdmin && !category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_AVAILABLE);
        }
        // 检查关联账单
        Long billCount = baseMapper.countBillsByCategory(categoryId);
        if (billCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_BILLS);
        }
        removeById(categoryId);
        log.info("删除分类 ID={}, name={}", categoryId, category.getName());
    }

    @Override
    @Transactional
    public void initSystemCategories() {
        // 检查是否已初始化
        if (count(new LambdaQueryWrapper<Category>().eq(Category::getUserId, 0L)) > 0) {
            return;
        }

        // 支出分类
        String[][] expenseCategories = {
                {"餐饮", "icon-food", "1"}, {"交通", "icon-transport", "2"},
                {"购物", "icon-shopping", "3"}, {"娱乐", "icon-entertainment", "4"},
                {"住房", "icon-housing", "5"}, {"医疗", "icon-medical", "6"},
                {"教育", "icon-education", "7"}, {"通讯", "icon-communication", "8"},
                {"服饰", "icon-clothing", "9"}, {"日用品", "icon-daily", "10"},
                {"丽人", "icon-beauty", "11"}, {"运动", "icon-sports", "12"},
                {"旅行", "icon-travel", "13"}, {"宠物", "icon-pet", "14"},
                {"数码", "icon-digital", "15"}, {"其他支出", "icon-other-expense", "99"}
        };
        for (String[] c : expenseCategories) {
            Category cat = new Category();
            cat.setUserId(0L);
            cat.setName(c[0]);
            cat.setIcon(c[1]);
            cat.setType("expense");
            cat.setSortOrder(Integer.parseInt(c[2]));
            save(cat);
        }

        // 收入分类
        String[][] incomeCategories = {
                {"工资", "icon-salary", "1"}, {"奖金", "icon-bonus", "2"},
                {"投资收益", "icon-investment", "3"}, {"兼职", "icon-parttime", "4"},
                {"红包", "icon-redpacket", "5"}, {"退款", "icon-refund", "6"},
                {"其他收入", "icon-other-income", "99"}
        };
        for (String[] c : incomeCategories) {
            Category cat = new Category();
            cat.setUserId(0L);
            cat.setName(c[0]);
            cat.setIcon(c[1]);
            cat.setType("income");
            cat.setSortOrder(Integer.parseInt(c[2]));
            save(cat);
        }
        log.info("系统默认分类初始化完成");
    }
}
