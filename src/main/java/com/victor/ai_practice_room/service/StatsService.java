package com.victor.ai_practice_room.service;


import com.victor.ai_practice_room.vo.StatsVo;

/**
 * 统计数据服务接口
 */
public interface StatsService {
    
    /**
     * 获取系统统计数据
     * @return 统计数据DTO
     */
    StatsVo getSystemStats();
} 