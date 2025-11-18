package com.victor.ai_practice_room.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.victor.ai_practice_room.entity.ExamRecord;
import com.victor.ai_practice_room.entity.Paper;
import com.victor.ai_practice_room.entity.PaperQuestion;
import com.victor.ai_practice_room.entity.Question;
import com.victor.ai_practice_room.exception.ExamException;
import com.victor.ai_practice_room.mapper.ExamRecordMapper;
import com.victor.ai_practice_room.mapper.PaperMapper;
import com.victor.ai_practice_room.mapper.PaperQuestionMapper;
import com.victor.ai_practice_room.mapper.QuestionMapper;
import com.victor.ai_practice_room.service.PaperQuestionService;
import com.victor.ai_practice_room.service.PaperService;
import com.victor.ai_practice_room.vo.PaperVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Autowired
    private PaperQuestionService paperQuestionService;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private ExamRecordMapper examRecordMapper;
    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public List<Paper> selectPaperByName(String name) {
        //通过试卷名称查找试卷信息
        List paperList = baseMapper.selectList(new LambdaQueryWrapper<Paper>().like(Paper::getName, name));
        return paperList;
    }

    @Override
    public boolean savePaperVo(PaperVo paperVo) {
        //根据题目名称查询试卷库中是否有同名试卷
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<Paper>().eq(Paper::getName, paperVo.getName()));
        if(exists){
            throw new ExamException(541,"试卷库中已经存在同名试卷\""+paperVo.getName()+"\"请修改试卷名称后再尝试保存");
        }
        //创建一个Paper对象
        Paper paper = new Paper();
        BeanUtils.copyProperties(paperVo,paper);
        //设置默认试卷状态为“草稿”
        paper.setStatus("DRAFT");
        Map<Integer, BigDecimal> questions = paperVo.getQuestions();
        paper.setQuestionCount(questions.size());
        //计算试卷总分
        BigDecimal totalScore = questions.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        //设置试卷总分
        paper.setTotalScore(totalScore);
        //调用自带的方法保存试卷
        baseMapper.insert(paper);
        //将Map<Integer,BigDecimal>转换为List<PaperQuestion>
         List<PaperQuestion> paperQuestionList = questions.entrySet().stream()
        .map(entry -> new PaperQuestion(Integer.valueOf(Math.toIntExact(paper.getId())), entry.getKey().longValue(), entry.getValue()))
        .collect(Collectors.toList());
        //批量保存方法
        paperQuestionService.saveBatch(paperQuestionList);
        return true;
    }

    @Override
    public boolean updatePaperById(PaperVo paperVo, Integer id) {
        //发布状态的试卷不能修改
        Paper paper = baseMapper.selectById(id);
        if("PUBLISHED".equalsIgnoreCase(paper.getStatus())){
            throw new ExamException(542,"发布状态中的试卷不能被修改");
        }
        //判断是否存在同名试卷
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<Paper>().eq(Paper::getName, paperVo.getName()).eq(Paper::getId, paper));
        if(exists){
            //抛出异常
            throw new ExamException(543,"试卷名称\""+paperVo.getName()+"\"已经存在,请修改试卷名称");
        }
        BeanUtils.copyProperties(paperVo,paper);
        paper.setUpdateTime(new Date());
        Map<Integer, BigDecimal> questions = paperVo.getQuestions();
        paper.setQuestionCount(questions.size());
        BigDecimal totalScore = questions.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        baseMapper.updateById(paper);
        //操作中间表
        //调用试卷id删除中间表数据
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getQuestionId,id));
        //将Map<Integer,BigDecimal>中转换为List<PaperQuestion>
        List<PaperQuestion> paperQuestionList = questions.entrySet().stream().map(entry -> new PaperQuestion(paper.getId().intValue(), entry.getKey().longValue(), entry.getValue())).collect(Collectors.toList());
        //调用PaperQuestionService中批量保存的方法
        paperQuestionService.saveBatch(paperQuestionList);
        return true;
    }

    @Override
    public boolean updatePaperStatus(Integer id, String status) {
        Long count = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getId, id).eq(ExamRecord::getStatus, "进行中"));
        if(count>0){
            throw new ExamException(544,"试卷正在被考试不能更新为草稿状态");
        }
        baseMapper.update(null,new LambdaUpdateWrapper<Paper>().eq(Paper::getId,id).set(Paper::getStatus,status));
        return true;
    }

    @Override
    public boolean deletePaperById(Integer id) {
        //如果试卷正在考试，不能删除
        Long count = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getId, id).eq(ExamRecord::getStatus, "进行中"));
        if(count>0){
            //抛出异常
            throw new ExamException(545,"试卷正在被考试，不能删除");
        }
        //调用自带的方法删除试卷
        baseMapper.deleteById(id);
        //删除中间表中的数据
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId,id));
        return true;
    }

    //试卷详情查询
    @Override
    public Paper getPaperDetailsById(Integer id) {
        Paper paper = baseMapper.selectById(id);
        //调用questionMapper中根据试卷id查询试卷中所有题目的方法
        List<Question> questionList = questionMapper.getQuestionListByPaperId(id);
        System.out.println(questionList);
        //对试卷中的题目按照选择、判断、简单的顺序排序
        questionList.sort(Comparator.comparing(Question::getType));
        //将所有的题目设置到试卷中
        paper.setQuestions(questionList);
        return paper;
    }


}