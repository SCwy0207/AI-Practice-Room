package com.victor.ai_practice_room.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.victor.ai_practice_room.entity.Question;
import com.victor.ai_practice_room.properties.AiProperties;
import com.victor.ai_practice_room.service.AIService;
import com.victor.ai_practice_room.vo.GradingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI服务实现类
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {
    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private WebClient webClient;

    @Override
    public String callAi(String prompt) {
        int maxTry = 3; //最多重试3次
        for (int i = 1; i <= 3; i++) {
            try {
                //请求体的内容 https://platform.moonshot.cn/docs/api/chat#%E8%AF%B7%E6%B1%82%E5%86%85%E5%AE%B9
                Map<String,String> userMap = new HashMap<>();
                userMap.put("role","user");
                userMap.put("content",prompt);
                List<Map> messagesList = new ArrayList<>();
                messagesList.add(userMap);

                Map<String,Object> requestBody = new HashMap<>();
                requestBody.put("model",aiProperties.getModel());
                requestBody.put("messages",messagesList);
                requestBody.put("temperature", aiProperties.getTemperature());
                requestBody.put("max_tokens", aiProperties.getMaxTokens());

                //2. 发起网络请求调用
                Mono<String> stringMono = webClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(100));
                //webClient异步请求
                //同步
                String result = stringMono.block();
                //jackson工具！ JsonObject JsonArray
                JSONObject resultJsonObject = JSONObject.parseObject(result);

                //错误结果：https://platform.moonshot.cn/docs/api/chat#错误说明
                if (resultJsonObject.containsKey("error")){
                    throw new RuntimeException("访问错误了，错误信息为:" +
                            resultJsonObject.getJSONObject("error").getString("message") );
                }
                //正确结果：https://platform.moonshot.cn/docs/api/chat#%E8%BF%94%E5%9B%9E%E5%86%85%E5%AE%B9
                //获取返回内容content
                // ```json  ```
                String content = resultJsonObject.getJSONArray("choices").getJSONObject(0).
                        getJSONObject("message").getString("content");
                log.debug("调用千问返回的结果为：{}",content);

                if (content == null || content.isEmpty()){
                    throw new RuntimeException("调用成功！但是没有返回结果！！");
                }
                return content;
            }catch (Exception e){
                //打印信息
                log.debug("第{}次尝试调用失败了！",i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                //                第几次尝试 i 次！
                if(i == maxTry){
                    e.printStackTrace();
                    throw new RuntimeException("已经重试3次！依然失败！请稍后再试！！");
                }
            }
        }
        throw new RuntimeException("已经重试3次！依然失败！请稍后再试！！");
    }

    @Override
    public GradingResult gradeTextQuestions(Question question, String userAnswer) {
        //1.生成ai调用的提示词
        String gradingPrompt = buildGradingPrompt(question, userAnswer);
        //2.调用ai模型，获取返回结果
        String content = callAi(gradingPrompt);
        //3.进行结果的解析 -》GradingResult
    /*
            prompt.append("{\n");
            prompt.append("  \"score\": 实际得分(整数),\n");
            prompt.append("  \"feedback\": \"具体的评价反馈(50字以内)\",\n");
            prompt.append("  \"reason\": \"扣分原因或得分依据(30字以内)\"\n");
            prompt.append("}");
         */
        JSONObject jsonObject = JSON.parseObject(content);
        Integer aiScore = jsonObject.getInteger("score");
        String feedback = jsonObject.getString("feedback");
        String reason = jsonObject.getString("reason");
        if (aiScore > question.getPaperScore().intValue()) aiScore = question.getPaperScore().intValue();
        if (aiScore < 0) aiScore = 0;
        return  new GradingResult(aiScore, feedback, reason);
    }

    @Override
    public String getExamAnswer(int totalScore, int totalPaperScore, int questionCount, AtomicInteger correctCount) {
        //1. 构建提示词
        String summaryPrompt  = buildSummaryPrompt(totalScore,totalPaperScore,questionCount,correctCount);
        //2. 调用kimiai
        String summary = callAi(summaryPrompt);
        //3. 结果解析
        return summary;
    }

    /**
     * 构建判卷提示词
     */
    private String buildGradingPrompt(Question question, String userAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的考试阅卷老师，请对以下题目进行判卷：\n\n");

        prompt.append("【题目信息】\n");
        prompt.append("题型：").append(getQuestionTypeText(question.getType())).append("\n");
        prompt.append("题目：").append(question.getTitle()).append("\n");
        prompt.append("标准答案：").append(question.getAnswer().getAnswer()).append("\n");
        prompt.append("满分：").append(question.getPaperScore().intValue()).append("分\n\n");

        prompt.append("【学生答案】\n");
        prompt.append(userAnswer.trim().isEmpty() ? "（未作答）" : userAnswer).append("\n\n");

        prompt.append("【判卷要求】\n");
        if ("CHOICE".equals(question.getType()) || "JUDGE".equals(question.getType())) {
            prompt.append("- 客观题：答案完全正确得满分，答案错误得0分\n");
        } else if ("TEXT".equals(question.getType())) {
            prompt.append("- 主观题：根据答案的准确性、完整性、逻辑性进行评分\n");
            prompt.append("- 答案要点正确且完整：80-100%分数\n");
            prompt.append("- 答案基本正确但不够完整：60-80%分数\n");
            prompt.append("- 答案部分正确：30-60%分数\n");
            prompt.append("- 答案完全错误或未作答：0分\n");
        }

        prompt.append("\n请严格按照以下规则输出：\n");
        prompt.append("- 只输出 JSON 内容，不要包含任何其他文字、解释、Markdown 代码块\n");
        prompt.append("- 不要使用 ```json ... ``` 包裹\n");
        prompt.append("- 确保 JSON 格式合法且可直接解析\n");
        prompt.append("- 不要添加注释或额外字段\n\n");

        prompt.append("{\n");
        prompt.append("  \"score\": 实际得分(整数),\n");
        prompt.append("  \"feedback\": \"具体的评价反馈(50字以内)\",\n");
        prompt.append("  \"reason\": \"扣分原因或得分依据(30字以内)\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 获取题目类型文本
     */
    private String getQuestionTypeText(String type) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("CHOICE", "选择题");
        typeMap.put("JUDGE", "判断题");
        typeMap.put("TEXT", "简答题");
        return typeMap.getOrDefault(type, "未知题型");
    }

    /**
     * 构建考试总评提示词
     */
    private String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount, AtomicInteger correctCount) {
        double percentage = (double) totalScore / maxScore * 100;

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深的教育专家，请为学生的考试表现提供专业的总评和学习建议：\n\n");

        prompt.append("【考试成绩】\n");
        prompt.append("总得分：").append(totalScore).append("/").append(maxScore).append("分\n");
        prompt.append("得分率：").append(String.format("%.1f", percentage)).append("%\n");
        prompt.append("题目总数：").append(questionCount).append("道\n");
        prompt.append("答对题数：").append(correctCount).append("道\n\n");

        prompt.append("【要求】\n");
        prompt.append("请提供一份150字左右的考试总评，包括：\n");
        prompt.append("1. 对本次考试表现的客观评价\n");
        prompt.append("2. 指出优势和不足之处\n");
        prompt.append("3. 提供具体的学习建议和改进方向\n");
        prompt.append("4. 给予鼓励和激励\n\n");

        prompt.append("请直接返回总评内容，无需特殊格式：");

        return prompt.toString();
    }
}