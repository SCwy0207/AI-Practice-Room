package com.victor.ai_practice_room.mapper;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.victor.ai_practice_room.entity.Category;
import com.victor.ai_practice_room.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.victor.ai_practice_room.vo.QuestionQueryVo;
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

    Question selectDetailsById(Long id);

    IPage<Question> getPageByCondition(Page<Question> questionPage, QuestionQueryVo questionQueryVo);

    boolean addQuestion(Question question);
} 