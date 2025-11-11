package com.victor.ai_practice_room.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExamException extends RuntimeException{
    private Integer code;
    private String msg;
}
