package com.victor.ai_practice_room.service.impl;

import com.victor.ai_practice_room.common.Result;
import com.victor.ai_practice_room.entity.Notice;
import com.victor.ai_practice_room.mapper.NoticeMapper;
import com.victor.ai_practice_room.service.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 公告服务实现类
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

} 