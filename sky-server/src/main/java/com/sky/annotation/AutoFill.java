package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识需要自动填充的字段
 */
@Target(ElementType.METHOD)// 表示该注解用于方法
@Retention(RetentionPolicy.RUNTIME)// 表示该注解在运行时保留
public @interface AutoFill {
    //数据库操作类型：插入INSERT、更新UPDATE
    OperationType value();
}
