package com.victor.ai_practice_room.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.victor.ai_practice_room.entity.PaperQuestion;
import com.victor.ai_practice_room.mapper.PaperQuestionMapper;
import com.victor.ai_practice_room.service.PaperQuestionService;
import org.springframework.stereotype.Service;

@Service
public class PaperQuestionServiceImpl extends ServiceImpl<PaperQuestionMapper, PaperQuestion> implements PaperQuestionService {
}
