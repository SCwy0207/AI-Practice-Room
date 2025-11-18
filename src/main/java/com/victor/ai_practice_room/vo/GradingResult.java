package com.victor.ai_practice_room.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public  class GradingResult {
    private Integer score;
    private String feedback;
    private String reason;
}