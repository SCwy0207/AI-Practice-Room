package com.victor.ai_practice_room.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.victor.ai_practice_room.properties.AiProperties;
import com.victor.ai_practice_room.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                //请求体的内容
                Map<String,String> userMap = new HashMap<>();
                userMap.put("role","user");
                userMap.put("content",prompt); //提示词
                List<Map> messagesList = new ArrayList<>();
                messagesList.add(userMap);

                Map<String,Object> requestBody = new HashMap<>();
                requestBody.put("model",aiProperties.getModel());
                requestBody.put("messages",messagesList);
                requestBody.put("temperature", aiProperties.getTemperature());
                requestBody.put("max_tokens", aiProperties.getMaxTokens());

                //2. 发起网络请求调用
                Mono<String> stringMono = webClient.post()
                        .uri("/chat/completions")  // 添加具体API路径
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())  // 添加认证头
                        .header("Content-Type", "application/json")  // 添加内容类型头
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
                log.debug("调用Qiwen返回的结果为：{}",content);

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
}