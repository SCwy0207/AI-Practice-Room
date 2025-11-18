package com.victor.ai_practice_room.service;

import com.victor.ai_practice_room.entity.ExamRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.victor.ai_practice_room.vo.ExamRankingVO;
import com.victor.ai_practice_room.vo.StartExamVo;
import com.victor.ai_practice_room.vo.SubmitAnswerVo;


import java.util.List;

/**
 * 考试记录Service接口
 * 定义考试记录相关的业务方法
 */
public interface ExamRecordService extends IService<ExamRecord> {

    ExamRecord startExam(StartExamVo startExamVo);

    boolean submitExamAnswers(Integer examRecordId, List<SubmitAnswerVo> answers);
    ExamRecord getExamRecordDetailsById(Integer id);
    boolean deleteExamRecordById(Integer id);

    List<ExamRankingVO> getExamRanking(Integer paperId, Integer limit);
}