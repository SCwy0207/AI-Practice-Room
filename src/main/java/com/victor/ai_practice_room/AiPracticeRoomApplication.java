package com.victor.ai_practice_room;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.victor.ai_practice_room.mapper")
public class AiPracticeRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPracticeRoomApplication.class, args);
    }

}
