package com.victor.ai_practice_room.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.victor.ai_practice_room.common.Result;
import com.victor.ai_practice_room.entity.Banner;
import com.victor.ai_practice_room.service.BannerService;
import com.victor.ai_practice_room.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 轮播图控制器 - 处理轮播图管理相关的HTTP请求
 * 包括图片上传、轮播图的CRUD操作、状态切换等功能
 */
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/banners")  // 轮播图API路径前缀
@CrossOrigin  // 允许跨域访问
@Tag(name = "轮播图管理", description = "轮播图相关操作，包括图片上传、轮播图增删改查、状态管理等功能")
public class BannerController {

    @Autowired
    BannerService bannerService;
    @Autowired
    FileUploadService fileUploadService;
    /**
     * 上传轮播图图片
     * @param file 图片文件
     * @return 图片访问URL
     */
    @PostMapping("/upload-image")  // 处理POST请求
    @Operation(summary = "上传轮播图图片", description = "将图片文件上传到MinIO服务器，返回可访问的图片URL")  // API描述
    public Result<String> uploadBannerImage(
            @Parameter(description = "要上传的图片文件，支持jpg、png、gif等格式，大小限制5MB") 
            @RequestParam("file") MultipartFile file) {
        return Result.success(fileUploadService.uploadFile(file), "图片上传成功");
    }
    
    /**
     * 获取启用的轮播图（前台首页使用）
     * @return 轮播图列表
     */
    @GetMapping("/active")  // 处理GET请求
    @Operation(summary = "获取启用的轮播图", description = "获取状态为启用的轮播图列表，供前台首页展示使用")  // API描述
    public Result<List<Banner>> getActiveBanners() {
        LambdaQueryWrapper<Banner> bannerLambdaQueryWrapper = new LambdaQueryWrapper<>();
        bannerLambdaQueryWrapper.eq(Banner::getIsActive,true);
        return Result.success(bannerService.list(bannerLambdaQueryWrapper),"获取启用的轮播图信息查询成功");
    }
    
    /**
     * 获取所有轮播图（管理后台使用）
     * @return 轮播图列表
     */
    @GetMapping("/list")  // 处理GET请求
    @Operation(summary = "获取所有轮播图", description = "获取所有轮播图列表，包括启用和禁用的，供管理后台使用")  // API描述
    public Result<List<Banner>> getAllBanners() {
        return Result.success(bannerService.list(),"获取所有轮播图查询成功");
    }
    
    /**
     * 根据ID获取轮播图
     * @param id 轮播图ID
     * @return 轮播图详情
     */
    @GetMapping("/{id}")  // 处理GET请求
    @Operation(summary = "根据ID获取轮播图", description = "根据轮播图ID获取单个轮播图的详细信息")  // API描述  
    public Result<Banner> getBannerById(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        Banner banner = bannerService.getById(id);
        if(banner==null){
            return Result.error("轮播图不存在");
        }
        return Result.success(banner,"轮播图查询成功");
    }
    
    /**
     * 添加轮播图
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PostMapping("/add")  // 处理POST请求
    @Operation(summary = "添加轮播图", description = "创建新的轮播图，需要提供图片URL、标题、跳转链接等信息")  // API描述
    public Result<String> addBanner(@RequestBody Banner banner) {
        return bannerService.save(banner)?Result.success("轮播图添加成功"):Result.error("轮播图添加失败");
    }
    
    /**
     * 更新轮播图
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PutMapping("/update")  // 处理PUT请求
    @Operation(summary = "更新轮播图", description = "更新轮播图的信息，包括图片、标题、跳转链接、排序等")  // API描述
    public Result<String> updateBanner(@RequestBody Banner banner) {
        return bannerService.saveOrUpdate(banner)?Result.success("轮播图更新成功"):Result.error("轮播图更新失败");
    }
    
    /**
     * 删除轮播图
     * @param id 轮播图ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")  // 处理DELETE请求
    @Operation(summary = "删除轮播图", description = "根据ID删除指定的轮播图")  // API描述
    public Result<String> deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        return bannerService.removeById(id)?Result.success("轮播图"+id+"删除成功"):Result.error("轮播图"+id+"删除失败");
    }
    
    /**
     * 启用/禁用轮播图
     * @param id 轮播图ID
     * @param isActive 是否启用
     * @return 操作结果
     */
    @PutMapping("/toggle/{id}")  // 处理PUT请求
    @Operation(summary = "切换轮播图状态", description = "启用或禁用指定的轮播图，禁用后不会在前台显示")  // API描述
    public Result<String> toggleBannerStatus(
            @Parameter(description = "轮播图ID") @PathVariable Long id, 
            @Parameter(description = "是否启用，true为启用，false为禁用") @RequestParam Boolean isActive) {
        LambdaUpdateWrapper<Banner> bannerLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        bannerLambdaUpdateWrapper.set(Banner::getIsActive,isActive).eq(Banner::getId,id);
        return bannerService.update(bannerLambdaUpdateWrapper)?Result.success("轮播图"+id+"状态更新成功"):Result.error("轮播图"+id+"状态更新失败");
    }
} 