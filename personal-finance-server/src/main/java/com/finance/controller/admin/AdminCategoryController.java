package com.finance.controller.admin;

import com.finance.annotation.AdminLog;
import com.finance.annotation.RequireSuperAdmin;
import com.finance.entity.Category;
import com.finance.service.CategoryService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员-分类管理
 * <p>权限：运营+超管可读；写操作仅超管。
 * <p>Bug 修复：原代码 deleteCategory 没有 !isAdmin 守卫，导致超管也无法删除系统分类。
 * 已在 CategoryServiceImpl 中加守卫，系统分类允许超管删除。
 */
@Tag(name = "管理员-分类管理", description = "全局分类的增删改查（写仅超管）")
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取全部分类")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Category> categories = categoryService.list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category c : categories) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("icon", c.getIcon());
            m.put("type", c.getType());
            m.put("sortOrder", c.getSortOrder());
            m.put("isSystem", c.getUserId() == 0);
            m.put("userId", c.getUserId());
            result.add(m);
        }
        return Result.ok(result);
    }

    @Operation(summary = "新增全局分类（仅超管）")
    @PostMapping
    @RequireSuperAdmin
    @AdminLog("新增全局分类")
    public Result<Map<String, Object>> add(@RequestBody Category category) {
        category.setUserId(0L); // 全局分类
        if (category.getSortOrder() == null) category.setSortOrder(0);
        categoryService.save(category);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", category.getId());
        m.put("name", category.getName());
        m.put("icon", category.getIcon());
        m.put("type", category.getType());
        m.put("sortOrder", category.getSortOrder());
        m.put("isSystem", true);
        return Result.ok("分类创建成功", m);
    }

    @Operation(summary = "修改分类（仅超管）")
    @PutMapping("/{id}")
    @RequireSuperAdmin
    @AdminLog("管理端修改分类")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Category category) {
        Category updated = categoryService.updateCategory(id, category, null, true);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", updated.getId());
        m.put("name", updated.getName());
        m.put("icon", updated.getIcon());
        m.put("type", updated.getType());
        m.put("sortOrder", updated.getSortOrder());
        return Result.ok("已修改", m);
    }

    @Operation(summary = "删除分类（仅超管）")
    @DeleteMapping("/{id}")
    @RequireSuperAdmin
    @AdminLog("管理端删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id, null, true);
        return Result.ok("分类已删除", null);
    }
}
