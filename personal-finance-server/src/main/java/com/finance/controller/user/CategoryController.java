package com.finance.controller.user;

import com.finance.entity.Category;
import com.finance.interceptor.JwtInterceptor;
import com.finance.service.CategoryService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "分类管理", description = "分类列表、自定义分类CRUD")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String type,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        List<Category> categories = categoryService.getUserCategories(userId, type);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category c : categories) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("icon", c.getIcon());
            m.put("type", c.getType());
            m.put("sortOrder", c.getSortOrder());
            m.put("isSystem", c.getUserId() == 0);
            result.add(m);
        }
        return Result.ok(result);
    }

    @Operation(summary = "创建自定义分类")
    @PostMapping
    public Result<Map<String, Object>> add(@RequestBody Category category, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Category saved = categoryService.addCustomCategory(category, userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("name", saved.getName());
        m.put("icon", saved.getIcon());
        m.put("type", saved.getType());
        m.put("sortOrder", saved.getSortOrder());
        m.put("isSystem", false);
        return Result.ok("分类创建成功", m);
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Category category,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Category updated = categoryService.updateCategory(id, category, userId, false);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", updated.getId());
        m.put("name", updated.getName());
        m.put("icon", updated.getIcon());
        m.put("type", updated.getType());
        m.put("sortOrder", updated.getSortOrder());
        m.put("isSystem", updated.getUserId() == 0);
        return Result.ok("已修改", m);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        categoryService.deleteCategory(id, userId, false);
        return Result.ok("分类已删除", null);
    }
}
