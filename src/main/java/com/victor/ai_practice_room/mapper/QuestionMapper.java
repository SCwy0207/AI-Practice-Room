package com.victor.ai_practice_room.mapper;


import com.victor.ai_practice_room.entity.Category;
import com.victor.ai_practice_room.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 题目Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础的CRUD操作
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
    @MapKey("category_id")
    List<Map<String, Long>>  getCategoriesCount();
} 