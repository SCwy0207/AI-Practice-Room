package com.victor.ai_practice_room.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.victor.ai_practice_room.entity.AnswerRecord;
import com.victor.ai_practice_room.mapper.AnswerRecordMapper;
import com.victor.ai_practice_room.service.AnswerRecordService;
import org.springframework.stereotype.Service;

@Service
public class AnswerRecordServiceImpl extends ServiceImpl<AnswerRecordMapper, AnswerRecord> implements AnswerRecordService {
}
