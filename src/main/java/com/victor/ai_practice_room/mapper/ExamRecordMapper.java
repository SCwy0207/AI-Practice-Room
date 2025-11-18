package com.victor.ai_practice_room.mapper;


import com.victor.ai_practice_room.entity.ExamRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.victor.ai_practice_room.vo.ExamRankingVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description 针对表【exam_record(考试记录表)】的数据库操作Mapper
 * @createDate 2025-06-20 22:37:43
 * @Entity com.victor.ai_practice_room.entity.ExamRecord
 */
@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    List<ExamRankingVO> getExamRanking(Integer paperId, Integer limit);
}