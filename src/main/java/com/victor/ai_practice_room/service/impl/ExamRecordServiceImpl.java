package com.victor.ai_practice_room.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.victor.ai_practice_room.entity.AnswerRecord;
import com.victor.ai_practice_room.entity.ExamRecord;
import com.victor.ai_practice_room.entity.Paper;
import com.victor.ai_practice_room.entity.Question;
import com.victor.ai_practice_room.exception.ExamException;
import com.victor.ai_practice_room.mapper.AnswerRecordMapper;
import com.victor.ai_practice_room.mapper.ExamRecordMapper;
import com.victor.ai_practice_room.service.AIService;
import com.victor.ai_practice_room.service.AnswerRecordService;
import com.victor.ai_practice_room.service.ExamRecordService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.victor.ai_practice_room.service.PaperService;
import com.victor.ai_practice_room.vo.ExamRankingVO;
import com.victor.ai_practice_room.vo.GradingResult;
import com.victor.ai_practice_room.vo.StartExamVo;
import com.victor.ai_practice_room.vo.SubmitAnswerVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.xml.crypto.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 考试记录Service实现类
 * 实现考试记录相关的业务逻辑
 */
@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {
    @Autowired
    private AnswerRecordService answerRecordService;
    @Autowired
    private AIService aiService;
    @Autowired
    private PaperService paperService;
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    //开始考试
    @Override
    public ExamRecord startExam(StartExamVo startExamVo) {
        //根据考生姓名，试卷id和考试状态查询考试记录
        LambdaQueryWrapper<ExamRecord> examRecordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        examRecordLambdaQueryWrapper
                //如果前端传来的学生姓名不为空（有内容），就加上一个查询条件：考试记录中的学生姓名等于这个传入的姓名；否则，就不加这个条件
                .eq(StringUtils.hasLength(startExamVo.getStudentName()),ExamRecord::getStudentName,startExamVo.getStudentName())
                .eq(startExamVo.getPaperId()!=null,ExamRecord::getExamId,startExamVo.getPaperId())
                .eq(ExamRecord::getStatus,"进行中");
        //调用带条件查询的方法
        ExamRecord examRecord = baseMapper.selectOne(examRecordLambdaQueryWrapper);
        if(examRecord==null){
            //如果没有查询到就创建新的考试记录
            examRecord= new ExamRecord();
            examRecord.setExamId(startExamVo.getPaperId());
            examRecord.setStudentName(startExamVo.getStudentName());
            examRecord.setStartTime(LocalDateTime.now());
            examRecord.setStatus("进行中");
            baseMapper.insert(examRecord);
        }
        return examRecord;
    }
    //获取试卷详情
    @Override
    public ExamRecord getExamRecordDetailsById(Integer id) {
        //调用自带的方法查询考试记录
        ExamRecord examRecord = baseMapper.selectById(id);
        //根据试卷id查询考试记录中的试卷信息
        Paper paperDetailsById = paperService.getPaperDetailsById(examRecord.getExamId());
        //将试卷设置到考试记录中
        examRecord.setPaper(paperDetailsById);
        //根据考试记录id查询用户的答题记录
        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId, id));
        //设置用户的答题记录
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }
    //提交试卷答案
    @Override
    public boolean submitExamAnswers(Integer examRecordId, List<SubmitAnswerVo> answers) {
        //根据考试id查询记录
        ExamRecord examRecord = baseMapper.selectById(examRecordId);
        if(examRecord!=null){
            //设置考试结束时间
            examRecord.setEndTime(LocalDateTime.now());
            examRecord.setStatus("已完成");
            baseMapper.updateById(examRecord);
            //保存用户的答题记录，将List<ExamRecord>转换为List<ExamRecordAnswer>
            List<AnswerRecord> answerRecordList = answers.stream().map(answer -> new AnswerRecord(examRecordId, answer.getQuestionId(), answer.getUserAnswer())).collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
            //调用判卷方法
            markingExamPaperById(examRecordId);
            return true;
        }
        return false;
    }

    private void markingExamPaperById(Integer examRecordId) {
        //根据考试记录id查询考试记录详情
        ExamRecord examRecordDetails = getExamRecordDetailsById(examRecordId);
        //获取试卷中的题目
        List<Question> questions = examRecordDetails.getPaper().getQuestions();
        //将List<Question>转换为Map<Long,Question>
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, question -> question));
        //获取用户的答题记录
        List<AnswerRecord> answerRecords = examRecordDetails.getAnswerRecords();
        //设置一个变量统计用户答对题目的数量
        AtomicInteger correctCount = new AtomicInteger();
        //遍历用户的答题记录
        answerRecords.forEach(answerRecord -> {
            //获取题目id
            Long questionId = Long.valueOf(answerRecord.getQuestionId());
            //根据题目id获取题目信息
            Question question = questionMap.get(questionId);
            //获取用户的答案
            String userAnswer = answerRecord.getUserAnswer();
            //对于非简单题，手动判断用户的答案是否正确
            if (!"TEXT".equals(question.getType())) {
                //对于判断题，数据库中存储的是T或F，需要转换为TRUE或FALSE
                if ("JUDGE".equals(question.getType())) {
                    userAnswer = userAnswer.equals("T") ? "TRUE" : "FALSE";
                }
                //判断用户的答案是否正确
                if (userAnswer.equals(question.getAnswer().getAnswer())) {
                    //设置用户答题记录中的得分为该题目在试卷中的分数
                    answerRecord.setScore(question.getPaperScore().intValue());
                    //设置用户答题记录中该题回答正确
                    answerRecord.setIsCorrect(1);
                    //对答对数量加1
                    correctCount.set(correctCount.get() + 1);
                } else {
                    //设置用户答题记录中的得分为该题目在试卷中的分数为0
                    answerRecord.setScore(0);
                    //设置用户答题记录中该题回答错误
                    answerRecord.setIsCorrect(0);
                }
            } else {
                //对于简答题，我们调用AI判卷
                GradingResult gradingResult = aiService.gradeTextQuestions(question, userAnswer);
                //获取AI的评分
                Integer score = gradingResult.getScore();
                //设置简答题的分数
                answerRecord.setScore(score);
                //判断简答题是否答对，如果得了满分，将isCorrect设置为1，如果得0分，设置为0，其他设置为2
                if (score.equals(0)) {
                    //该简答题没有得分
                    answerRecord.setIsCorrect(0);
                    //设置ai批改意见
                    answerRecord.setAiCorrection(gradingResult.getReason());
                } else if (score.equals(question.getPaperScore().intValue())) {
                    //该简单题得了满分
                    answerRecord.setIsCorrect(1);
                    //设置ai批改意见
                    answerRecord.setAiCorrection(gradingResult.getFeedback());
                    //对答对数量加1
                    correctCount.set(correctCount.get() + 1);
                } else {
                    //得了一部分分数
                    answerRecord.setIsCorrect(2);
                    //设置ai批改意见
                    answerRecord.setAiCorrection(gradingResult.getReason());
                    //对答对数量加1
                    correctCount.set(correctCount.get() + 1);
                }
            }
            //更新用户答题记录
            answerRecordMapper.updateById(answerRecord);
        });
        //获取当前试卷所有题目的总分
        int totalPaperScore = questions.stream().mapToInt(question -> question.getPaperScore().intValue()).sum();
        //计算用户的总得分
        int totalScore = answerRecords.stream().filter(answerRecord -> answerRecord.getIsCorrect() != 0).mapToInt(AnswerRecord::getScore).sum();
        //设置当前试卷的总分数
        examRecordDetails.setScore(totalScore);
        //调用AI给出当前考试的评语
        //设置考试记录的状态
        examRecordDetails.setStatus("已批阅");
        //调用AI获取考试评语
        String answer = aiService.getExamAnswer(totalScore,totalPaperScore,questions.size(),correctCount);
        //设置AI评语
        examRecordDetails.setAnswers(answer);
        //更新考试记录
        baseMapper.updateById(examRecordDetails);
    }
    //删除考试记录
    @Override
    public boolean deleteExamRecordById(Integer id) {
        //进行中状态的考试记录不能删除
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getStatus, "进行中").eq(ExamRecord::getId,id));
        if(exists){
            //抛出异常
            throw new ExamException(561,"进行中状态的考试记录不能删除");
        }
        //删除考试记录
        baseMapper.deleteById(id);
        //删除答题记录
        answerRecordMapper.delete(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId,id));
        return true;
    }

    @Override
    public List<ExamRankingVO> getExamRanking(Integer paperId, Integer limit) {
        return baseMapper.getExamRanking(paperId,limit);
    }


}