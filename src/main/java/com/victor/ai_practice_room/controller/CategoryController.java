package com.victor.ai_practice_room.controller;

import com.victor.ai_practice_room.common.Result;
import com.victor.ai_practice_room.entity.Category;
import com.victor.ai_practice_room.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器 - 处理题目分类管理相关的HTTP请求
 * 包括分类的增删改查、树形结构展示等功能
 */
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/categories")  // 分类API路径前缀
@Tag(name = "分类管理", description = "题目分类相关操作，包括分类的增删改查、树形结构管理等功能")  // Swagger API分组
public class CategoryController {
    @Autowired
    CategoryService categoryService;

    /**
     * 获取分类列表（包含题目数量）
     * @return 分类列表数据
     */
    @GetMapping  // 处理GET请求
    @Operation(summary = "获取分类列表", description = "获取所有题目分类列表，包含每个分类下的题目数量统计")  // API描述
    public Result<List<Category>> getCategories() {
        return Result.success(categoryService.getAllCategories(),"成功获取分类列表");
    }

    /**
     * 获取分类树形结构
     * @return 分类树数据
     */
    @GetMapping("/tree")  // 处理GET请求
    @Operation(summary = "获取分类树形结构", description = "获取题目分类的树形层级结构，用于前端树形组件展示")  // API描述
    public Result<List<Category>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree(),"查询分类树结构成功");
    }

    /**
     * 添加分类
     * @param category 分类对象
     * @return 操作结果
     */
    @PostMapping  // 处理POST请求
    @Operation(summary = "添加新分类", description = "创建新的题目分类，支持设置父分类实现层级结构")  // API描述
    public Result<Boolean> addCategory(@RequestBody Category category) {
        return Result.success(categoryService.save(category),"添加新分类"+category.getName()+"成功");
    }

    /**
     * 更新分类
     * @param category 分类对象
     * @return 操作结果
     */
    @PutMapping  // 处理PUT请求
    @Operation(summary = "更新分类信息", description = "修改分类的名称、描述、排序等信息")  // API描述
    public Result<Boolean> updateCategory(@RequestBody Category category) {
        return Result.success(categoryService.update(category),"更新分类"+category.getName()+"信息成功");
    }

    /**
     * 删除分类
     * @param id 分类ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")  // 处理DELETE请求
    @Operation(summary = "删除分类", description = "删除指定的题目分类，注意：删除前需确保分类下没有题目")  // API描述
    public Result<Boolean> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        return Result.success(categoryService.deleteById(id),"删除分类"+id+"成功");
    }
} 