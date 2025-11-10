package com.victor.ai_practice_room.service.impl;

import com.victor.ai_practice_room.entity.ExamRecord;
import com.victor.ai_practice_room.mapper.ExamRecordMapper;
import com.victor.ai_practice_room.service.ExamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 考试服务实现类
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {

} 