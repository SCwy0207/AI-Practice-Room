package com.victor.ai_practice_room;

import com.victor.ai_practice_room.properties.AiProperties;
import com.victor.ai_practice_room.properties.MinioProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.victor.ai_practice_room.mapper")
@EnableConfigurationProperties(value = {MinioProperties.class, AiProperties.class})
public class AiPracticeRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPracticeRoomApplication.class, args);
    }

}
