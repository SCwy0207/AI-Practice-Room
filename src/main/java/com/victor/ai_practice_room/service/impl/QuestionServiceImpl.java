package com.victor.ai_practice_room.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.victor.ai_practice_room.entity.*;
import com.victor.ai_practice_room.exception.ExamException;
import com.victor.ai_practice_room.mapper.*;
import com.victor.ai_practice_room.service.QuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.victor.ai_practice_room.vo.QuestionQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    //获取指定ID的题目完整信息，包括题目内容、选项、答案等详细数据
    @Override
    public Question selectDetailsById(Long id) {
        //根据id查询题目详情信息
        Question question = baseMapper.selectDetailsById(id);
        //创建一个线程，向Redis中存放热门题目数据
        new Thread(()->{
            redisTemplate.opsForZSet().incrementScore("question:popular",id,1);
        }).start();
        return question;
    }

    @Override
    public IPage<Question> getPageByCondition(Page<Question> questionPage, QuestionQueryVo questionQueryVo) {
        return baseMapper.getPageByCondition(questionPage,questionQueryVo);
    }

    @Override
    public boolean addQuestion(Question question) {
        //判断该类型下是否存在相同的题目
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<Question>().eq(Question::getTitle, question.getTitle()).eq(Question::getType, question.getType()).eq(Question::getCategoryId, question.getCategoryId()));
        if(exists){
            //获取分类名
            Category categoryName = categoryMapper.selectById(question.getCategoryId());
            throw new ExamException(451,"在<"+categoryName+">中已经存在名为:"+question.getTitle()+"的题目");
        }
        //如果题目不存在，则开始正常插入流程
        //插入题目信息
        baseMapper.insert(question);
        //获取数据库生成的题目id,当执行了当执行 baseMapper.insert(question) 后MyBatis-Plus的自动ID回填机制会自动将数据库生成的ID回填到原始对象上
        Long questionId = question.getId();
        //获取题目的答案
        QuestionAnswer answer = question.getAnswer();
        //设置题目的id
        answer.setQuestionId(questionId);
        //如果question是选择题则获取所有的选项
        if("CHOICE".equalsIgnoreCase(question.getType())){
            List<QuestionChoice> choices = question.getChoices();
            if(!CollectionUtils.isEmpty(choices)){
                //创建一个StringBuilder对象
                StringBuilder stringBuilder = new StringBuilder();
                AtomicInteger index = new AtomicInteger(0);
                //遍历选项
                choices.forEach(choice->{
                    int currentIndex = index.getAndIncrement();
                            //设置题目id
                    choice.setQuestionId(questionId);
                    choice.setSort(currentIndex);
                    //判断当前选项是否是正确答案
                    if(choice.getIsCorrect()){
                        if(stringBuilder.length()>0){
                            //拼接逗号分隔
                            stringBuilder.append(",");
                        }
                        stringBuilder.append((char)(65+currentIndex));
                    }
                    questionChoiceMapper.insert(choice);
                });
                answer.setAnswer(stringBuilder.toString());
            }
        }
        questionAnswerMapper.insert(answer);
        return true;
    }

    @Override
    public boolean updateQuestion(Question question) {
        //判断当前题目是否存在
        boolean exists = baseMapper.exists(new LambdaQueryWrapper<Question>().eq(Question::getTitle, question.getTitle()).eq(Question::getType, question.getType()).eq(Question::getId, question.getId()).eq(Question::getCategoryId, question.getCategoryId()).ne(Question::getId, question.getId()));
        if(exists){
            Category category = categoryMapper.selectById(question.getCategoryId());
            throw new ExamException(452,"在<"+category.getName()+">下已经存在名为"+question.getTitle()+"的题目,请修改题目名称");
        }
        //如果不存在，则将题目的更新日期设置为null，这样数据库会自动根据当前时间设置
        question.setUpdateTime(null);
        baseMapper.updateById(question);
        //获取题目的答案
        QuestionAnswer answer = question.getAnswer();
        answer.setUpdateTime(null);
        if("CHOICE".equalsIgnoreCase(question.getType())){
            questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,question.getId()));
            //获取所有选项
            List<QuestionChoice> choices = question.getChoices();
            if(!CollectionUtils.isEmpty(choices)){
                StringBuilder stringBuilder = new StringBuilder();
                AtomicInteger atomicInteger = new AtomicInteger();
                choices.forEach(choice ->{
                    int index = atomicInteger.getAndIncrement();
                    choice.setId(null);
                    choice.setCreateTime(null);
                    choice.setUpdateTime(null);
                    //判断当前选项是否是正确答案
                    if(choice.getIsCorrect()){
                        if(stringBuilder.length()>0){
                            stringBuilder.append(",");
                        }
                        stringBuilder.append((char)(65+index));
                    }
                    questionChoiceMapper.insert(choice);
                });
                answer.setAnswer(stringBuilder.toString());
            }
        }
        questionAnswerMapper.updateById(answer);
        return true;
    }

    @Override
    public boolean deleteQuestionById(Long id) {
        //判断当前题目是否存在于某个试卷中
        boolean exists = paperQuestionMapper.exists(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getQuestionId, id));
        if(exists){
            throw new ExamException(453,"该题目存在于某个试卷中，不能删除");
        }
        //删除题目
        baseMapper.deleteById(id);
        //删除题目的答案
        questionAnswerMapper.delete(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId, id));
        //删除题目的选项
        questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,id));
        return true;
    }

    @Override
    public List<Question> getPopularQuestions(Integer size) {
        //创建一个返回的集合
        List<Question> popularQuestions = new ArrayList<>();
        //获取Redis中的热门题目以及对应分数(根据评分高低排序)
        Set<ZSetOperations.TypedTuple<Object>> redisPopularQuestions = redisTemplate.opsForZSet().reverseRangeWithScores("question:popular", 0, -1);
        if(!CollectionUtils.isEmpty(redisPopularQuestions)){
            //Redis中存在热门题目，遍历所有热门题目的id
            redisPopularQuestions.forEach(tuple->{
                // 从 tuple 中获取题目 ID（字符串形式）
                Object value = tuple.getValue();
                String questionIdStr = String.valueOf(value);
                // 转换为 Long 类型
                Long questionId = Long.valueOf(questionIdStr);
                // 使用 ID 查询题目详情
                Question questionDetail = baseMapper.selectById(questionId);
                // 添加到结果集合中
                if (questionDetail != null) {
                    popularQuestions.add(questionDetail);
                }
            });
        }
        //判断Redis中热门题目中的数量是否满足前端需要的热门数量的题目
        Integer diff = size - redisPopularQuestions.size();
        if(diff>0){
            //Redis中的热门的题目的数量不足，需要从数据库中查询最新的非Redis中的热门的题目
            //创建LabmdaQueryWrapper对象
            LambdaQueryWrapper<Question> questionLambdaQueryWrapper = new LambdaQueryWrapper<>();
            //封装查询条件
            //1.按照创建试卷倒叙排序
            questionLambdaQueryWrapper.orderByDesc(Question::getCreateTime);
            //2.id不在Redis中
            // 从 redisPopularQuestions 中提取所有的题目 ID
            List<Long> redisQuestionIds = redisPopularQuestions.stream()
                    .map(tuple -> {
                        Object value = tuple.getValue();
                        return Long.valueOf(String.valueOf(value));
                    })
                    .collect(Collectors.toList());
            questionLambdaQueryWrapper.notIn(redisPopularQuestions.size() > 0 ,Question::getId,redisQuestionIds);
            //3.在整个SQL后面添加一个 limit diff
            questionLambdaQueryWrapper.last("limit "+diff);
            //调用baseMapper中带条件查询的方法
            List<Question> databaseQuestions = baseMapper.selectList(questionLambdaQueryWrapper);
            //将从数据库中补充的热门题目添加到返回的集合中
            popularQuestions.addAll(databaseQuestions);
        }
        //给所有的热门题目添加答案和选项
        fillQuestionAnswerAndChoice(popularQuestions);
        return popularQuestions;
    }
    //给题目设置题目答案和题目选项的方法
    private void fillQuestionAnswerAndChoice(List<Question> records) {
        if (!CollectionUtils.isEmpty(records)) {
            //获取所有题目的id
            List<Long> qustionIds = records.stream().map(Question::getId).collect(Collectors.toList());
            //调用QuestionAnswerMapper中的方法查询所有题目的答案
            List<QuestionAnswer> questionAnswers = questionAnswerMapper.selectList(new LambdaQueryWrapper<QuestionAnswer>().in(QuestionAnswer::getQuestionId, qustionIds));
            //调用QuestionChoiceMapper中的方法查询所有选择题的选项
            List<QuestionChoice> questionChoices = questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>().in(QuestionChoice::getQuestionId, qustionIds));
            //将List<QuestionAnswer>转换为Map<Long,QuestionAnswer>
            Map<Long, QuestionAnswer> questionAnswerMap = questionAnswers.stream().collect(Collectors.toMap(
                    QuestionAnswer::getQuestionId,
                    qa -> qa
            ));
            //将List<QuestionChoice>转换为Map<Long,List<QuestionChoice>>
            Map<Long, List<QuestionChoice>> questionChoicesMap = questionChoices.stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));

            //遍历records中的所有题目
            records.forEach(question -> {
                //获取当前题目的id
                Long questionId = question.getId();
                //从questionAnswerMap中获取题目的答案
                QuestionAnswer questionAnswer = questionAnswerMap.get(questionId);
                //将问题的答案设置到题目中
                question.setAnswer(questionAnswer);
                //判断是否是选择题
                if ("CHOICE".equals(question.getType())) {
                    //从questionChoicesMap中获取选择题的所有选项
                    List<QuestionChoice> questionChoices1 = questionChoicesMap.get(questionId);
                    if (!CollectionUtils.isEmpty(questionChoices1)) {
                        //按照选项中的sort进行升序排序
                        questionChoices1.sort(Comparator.comparing(QuestionChoice::getSort));
                        //将选项设置到题目中
                        question.setChoices(questionChoices1);
                    }
                }
            });
        }
    }
}