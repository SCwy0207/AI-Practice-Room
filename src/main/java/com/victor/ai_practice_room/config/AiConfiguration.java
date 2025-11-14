package com.victor.ai_practice_room.config;

import com.victor.ai_practice_room.properties.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootConfiguration
public class AiConfiguration {

    @Autowired
    private AiProperties aiProperties;

    @Bean
    public WebClient webClient(){
        //1.创建WebClient对象
        WebClient webClient = WebClient.builder()
                //设置基础路径
                .baseUrl(aiProperties.getBaseUrl())
                //设置请求数据的类型
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                //设置api_key进行认证
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+aiProperties.getApiKey())
                .build();
        return webClient;
    }
}