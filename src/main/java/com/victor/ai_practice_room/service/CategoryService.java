package com.victor.ai_practice_room.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.victor.ai_practice_room.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> getAllCategories();

    List<Category> getCategoryTree();

    Boolean update(Category category);

    Boolean deleteById(Long id);
}