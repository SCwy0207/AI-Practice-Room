package com.victor.ai_practice_room.mapper;


import com.victor.ai_practice_room.entity.Question;
import com.victor.ai_practice_room.entity.QuestionChoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 题目选项
 */
@Mapper
public interface QuestionChoiceMapper extends BaseMapper<QuestionChoice> {
    List<QuestionChoice> getQuestionAnswerByCondition(Long questionId);
} 