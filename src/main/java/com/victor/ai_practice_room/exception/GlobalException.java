package com.victor.ai_practice_room.exception;

import com.victor.ai_practice_room.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@RestControllerAdvice
public class GlobalException {
    //处理异常的方法
    @ExceptionHandler(Exception.class)
    public Result doResolveException(Exception e){
        e.printStackTrace();
        log.error(e.getMessage());
        return Result.error(e.getMessage());
    }
    //处理ExamException异常的方法
    @ExceptionHandler(ExamException.class)
    public Result doExamResolveException(ExamException e){
        e.printStackTrace();
        log.error(e.getMessage());
        return Result.error(e.getMessage());
    }

}
