package com.victor.ai_practice_room.service;


import com.victor.ai_practice_room.entity.Question;
import com.victor.ai_practice_room.vo.GradingResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI服务接口
 */
public interface AIService {

    String callAi(String prompt);

    GradingResult gradeTextQuestions(Question question, String userAnswer);

    String getExamAnswer(int totalScore, int totalPaperScore, int questionCount, AtomicInteger correctCount);
}