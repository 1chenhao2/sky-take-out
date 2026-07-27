package com.sky.handler;

import com.sky.exception.AiServiceException;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
    @ExceptionHandler
    public Result exceptionHandler(AiServiceException ex){
        log.error("AI服务异常：{}", ex.getMessage());
        return Result.error("AI服务异常: " + ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(IOException ex){
        log.error("IO异常：{}", ex.getMessage());
        return Result.error("服务通信异常，请稍后重试");
    }

    /**
     * 处理数据重复异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        if (message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            String username = split[2].substring(0, split[2].length() - 1);
            return Result.error("用户名【" + username + "】已存在");
        } else {
            return Result.error("未知错误");
        }
    }

}
