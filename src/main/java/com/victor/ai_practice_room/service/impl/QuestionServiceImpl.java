package com.victor.ai_practice_room.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.victor.ai_practice_room.entity.*;
import com.victor.ai_practice_room.exception.ExamException;
import com.victor.ai_practice_room.mapper.*;
import com.victor.ai_practice_room.service.AIService;
import com.victor.ai_practice_room.service.QuestionService;
import com.victor.ai_practice_room.utils.ExcelUtil;
import com.victor.ai_practice_room.vo.AiGenerateRequestVo;
import com.victor.ai_practice_room.vo.QuestionImportVo;
import com.victor.ai_practice_room.vo.QuestionQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    @Autowired
    private AIService aiService;

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
                    choice.setQuestionId(question.getId());
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
    //Excel预览功能
    @Override
    public List<QuestionImportVo> previewExcel(MultipartFile file) throws IOException {
        //判断文件是否为空
        if(file.isEmpty()||file.getSize()==0){
            throw new ExamException(454,"上传模板文件为空,请检查后重新上传文件");
        }
        //判断是否为Excel文件
        if(!file.getOriginalFilename().endsWith(".xls")&& !file.getOriginalFilename().endsWith(".xlsx")){
            throw new ExamException(455,"文件格式不正确，请重新上传excel文件");
        }
        //调用ExcelUtil工具类中的方法解析Excel文件
        List<QuestionImportVo> questionImportVos = ExcelUtil.parseExcel(file);
        return questionImportVos;
    }
    //导入题目数据
    @Override
    public Integer importQuestions(List<QuestionImportVo> questionImportVos) {
        //判断是否有数据
        if(CollectionUtils.isEmpty(questionImportVos)){
            //没有数据，抛出异常
            throw new ExamException(456,"没有导入任何题目,请检查数据");
        }
        //遍历所有的题目
        AtomicInteger successCount = new AtomicInteger();
        questionImportVos.forEach(questionImportVo ->{
            //将questionImportVo转换为Question类型
            Question question =convertQuestionImportVoToQuestion(questionImportVo);
            //调用自定义的方法向数据库中插入题目、题目答案、题目选项
            addQuestion(question);
            successCount.getAndIncrement();
        });
        return successCount.get();
    }

    @Override
    public List<QuestionImportVo> generateQuestionsByAi(AiGenerateRequestVo request) {
        //1.生成提示词
        String prompt = buildPrompt(request);
        //2.调用AI大模型
        String content = aiService.callAi(prompt);
        //3.解析结果
        int startIndex = content.indexOf("```json");
        int endIndex = content.lastIndexOf("```");
        //保证有数据，且下标正确！
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            //获取真正结果
            String realResult = content.substring(startIndex+7,endIndex);
            System.out.println("realResult = " + realResult);
            JSONObject jsonObject = JSONObject.parseObject(realResult);
            JSONArray questions = jsonObject.getJSONArray("questions");
            List<QuestionImportVo> questionImportVoList = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                //获取对象
                JSONObject questionJson = questions.getJSONObject(i);
                QuestionImportVo questionImportVo = new QuestionImportVo();
                questionImportVo.setTitle(questionJson.getString("title"));
                questionImportVo.setType(questionJson.getString("type"));
                questionImportVo.setMulti(questionJson.getBoolean("multi"));
                questionImportVo.setDifficulty(questionJson.getString("difficulty"));
                questionImportVo.setScore(questionJson.getInteger("score"));
                questionImportVo.setAnalysis(questionJson.getString("analysis"));
                questionImportVo.setCategoryId(request.getCategoryId());

                //选择题处理选项
                if ("CHOICE".equals(questionImportVo.getType())) {
                    JSONArray choices = questionJson.getJSONArray("choices");
                    List<QuestionImportVo.ChoiceImportDto> choiceImportDtoList = new ArrayList<>(choices.size());
                    for (int i1 = 0; i1 < choices.size(); i1++) {
                        JSONObject choicesJSONObject = choices.getJSONObject(i1);
                        QuestionImportVo.ChoiceImportDto choiceImportDto = new QuestionImportVo.ChoiceImportDto();
                        choiceImportDto.setContent(choicesJSONObject.getString("content"));
                        choiceImportDto.setIsCorrect(choicesJSONObject.getBoolean("isCorrect"));
                        choiceImportDto.setSort(choicesJSONObject.getInteger("sort"));
                        choiceImportDtoList.add(choiceImportDto);
                    }
                    questionImportVo.setChoices(choiceImportDtoList);
                }
                //答案 [判断题！ TRUE |FALSE  false true  f  t 是 否]
                questionImportVo.setAnswer(questionJson.getString("answer"));
                questionImportVoList.add(questionImportVo);
            }
            return questionImportVoList;
        }
        throw new RuntimeException("ai生成题目json数据结构错误，无法正常解析！数据为：%s".formatted(content));
    }


    //将questionImportVo转换为Question类型
    private Question convertQuestionImportVoToQuestion(QuestionImportVo questionImportVo) {
        //创建一个Question对象
        Question question = new Question();
        //调用BeanUtils工具类中的方法复制相同属性的属性值
        BeanUtils.copyProperties(questionImportVo,question);
        //创建QuestionAnswer对象
        QuestionAnswer questionAnswer = new QuestionAnswer();
        //对于判断题和简答题设置答案
        if("JUDGE".equalsIgnoreCase(questionImportVo.getType())){
            //如果是判断题，将问题的答案转为大写
            questionAnswer.setAnswer(questionImportVo.getAnswer().toUpperCase());
        }else{
            questionAnswer.setAnswer(questionImportVo.getAnswer());
        }
        //设置keywords
        questionAnswer.setKeywords(questionImportVo.getKeywords());
        //给题目设置答案
        question.setAnswer(questionAnswer);
        //判断是否是选择题
        if("CHOICE".equalsIgnoreCase(questionImportVo.getType())){
            //获取所有的选项
            List<QuestionImportVo.ChoiceImportDto> choices = questionImportVo.getChoices();
            //创建一个List<QuestionChoice>集合
            List<QuestionChoice> questionChoices = new ArrayList<>();
            if(!CollectionUtils.isEmpty(choices)){
                choices.forEach(choice->{
                    QuestionChoice questionChoice = new QuestionChoice();
                    BeanUtils.copyProperties(choice,questionChoice);
                    questionChoices.add(questionChoice);
                });
            }else{
                throw new ExamException(456,"选项为空，");
            }
            question.setChoices(questionChoices);
        }
        return question;
    }
    public String buildPrompt(AiGenerateRequestVo request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为我生成").append(request.getCount()).append("道关于【")
                .append(request.getTopic()).append("】的题目。\n\n");

        prompt.append("要求：\n");

        // 题目类型要求
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            List<String> typeList = Arrays.asList(request.getTypes().split(","));
            prompt.append("- 题目类型：");
            for (String type : typeList) {
                switch (type.trim()) {
                    case "CHOICE":
                        prompt.append("选择题");
                        if (request.getIncludeMultiple() != null && request.getIncludeMultiple()) {
                            prompt.append("(包含单选和多选)");
                        }
                        prompt.append(" ");
                        break;
                    case "JUDGE":
                        prompt.append("判断题（**重要：确保正确答案和错误答案的数量大致平衡，不要全部都是正确或错误**） ");
                        break;
                    case "TEXT":
                        prompt.append("简答题 ");
                        break;
                }
            }
            prompt.append("\n");
        }

        // 难度要求
        if (request.getDifficulty() != null) {
            String difficultyText = switch (request.getDifficulty()) {
                case "EASY" -> "简单";
                case "MEDIUM" -> "中等";
                case "HARD" -> "困难";
                default -> "中等";
            };
            prompt.append("- 难度等级：").append(difficultyText).append("\n");
        }

        // 额外要求
        if (request.getRequirements() != null && !request.getRequirements().isEmpty()) {
            prompt.append("- 特殊要求：").append(request.getRequirements()).append("\n");
        }

        // 判断题特别要求
        if (request.getTypes() != null && request.getTypes().contains("JUDGE")) {
            prompt.append("- **判断题特别要求**：\n");
            prompt.append("  * 确保生成的判断题中，正确答案(TRUE)和错误答案(FALSE)的数量尽量平衡\n");
            prompt.append("  * 不要所有判断题都是正确的或都是错误的\n");
            prompt.append("  * 错误的陈述应该是常见的误解或容易混淆的概念\n");
            prompt.append("  * 正确的陈述应该是重要的基础知识点\n");
        }

        prompt.append("\n请严格按照以下JSON格式返回，不要包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"题目内容\",\n");
        prompt.append("      \"type\": \"CHOICE|JUDGE|TEXT\",\n");
        prompt.append("      \"multi\": true/false,\n");
        prompt.append("      \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
        prompt.append("      \"score\": 5,\n");
        prompt.append("      \"choices\": [\n");
        prompt.append("        {\"content\": \"选项内容\", \"isCorrect\": true/false, \"sort\": 1}\n");
        prompt.append("      ],\n");
        prompt.append("      \"answer\": \"TRUE或FALSE(判断题专用)|文本答案(简答题专用)\",\n");
        prompt.append("      \"analysis\": \"题目解析\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("注意：\n");
        prompt.append("1. 选择题必须有choices数组，判断题和简答题设置answer字段\n");
        prompt.append("2. 多选题的multi字段设为true，单选题设为false\n");
        prompt.append("3. **判断题的answer字段只能是\"TRUE\"或\"FALSE\"，请确保答案分布合理**\n");
        prompt.append("4. 每道题都要有详细的解析\n");
        prompt.append("5. 题目要有实际价值，贴近实际应用场景\n");
        prompt.append("6. 严格按照JSON格式返回，确保可以正确解析\n");

        // 如果只生成判断题，额外强调答案平衡
        if (request.getTypes() != null && request.getTypes().equals("JUDGE") && request.getCount() > 1) {
            prompt.append("7. **判断题答案分布要求**：在").append(request.getCount()).append("道判断题中，");
            int halfCount = request.getCount() / 2;
            if (request.getCount() % 2 == 0) {
                prompt.append("请生成").append(halfCount).append("道正确(TRUE)和").append(halfCount).append("道错误(FALSE)的题目");
            } else {
                prompt.append("请生成约").append(halfCount).append("-").append(halfCount + 1).append("道正确(TRUE)和约").append(halfCount).append("-").append(halfCount + 1).append("道错误(FALSE)的题目");
            }
        }

        return prompt.toString();
    }


}