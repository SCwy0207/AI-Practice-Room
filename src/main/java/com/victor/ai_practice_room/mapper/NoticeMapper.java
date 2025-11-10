package com.victor.ai_practice_room.mapper;

import com.victor.ai_practice_room.entity.Notice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告Mapper接口
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
    
} 