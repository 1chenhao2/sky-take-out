package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，用于实现公共字段自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPoint(){}
    /**
     * 前置通知，在方法执行前进行执行
     */
    @Before("autoFillPoint()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行自动填充");

        try {
            // 获取切点方法的参数
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                log.warn("切点方法参数为空，跳过自动填充");
                return;
            }

            // 获取当前被拦截的方法对象
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 获取方法上的 @AutoFill 注解
            AutoFill autoFill = method.getAnnotation(AutoFill.class);
            if (autoFill == null) {
                log.warn("方法上未找到 @AutoFill 注解，跳过自动填充");
                return;
            }

            // 获取数据库操作类型
            OperationType operationType = autoFill.value();
            log.info("检测到数据库操作类型: {}", operationType);

            // 获取方法参数中的实体对象（通常是第一个参数）
            Object entity = args[0];
            if (entity == null) {
                log.warn("实体对象为空，跳过自动填充");
                return;
            }

            // 根据操作类型设置不同的公共字段
            LocalDateTime now = LocalDateTime.now();
            Long currentId = BaseContext.getCurrentId();

            if (operationType == OperationType.INSERT) {
                // 插入操作：设置创建时间和更新时间、创建人和更新人
                Field createTime = entity.getClass().getDeclaredField("createTime");
                createTime.setAccessible(true);
                createTime.set(entity, now);

                Field updateTime = entity.getClass().getDeclaredField("updateTime");
                updateTime.setAccessible(true);
                updateTime.set(entity, now);

                Field createUser = entity.getClass().getDeclaredField("createUser");
                createUser.setAccessible(true);
                createUser.set(entity, currentId);

                Field updateUser = entity.getClass().getDeclaredField("updateUser");
                updateUser.setAccessible(true);
                updateUser.set(entity, currentId);
            } else if (operationType == OperationType.UPDATE) {
                // 更新操作：只设置更新时间和更新人
                Field updateTime = entity.getClass().getDeclaredField("updateTime");
                updateTime.setAccessible(true);
                updateTime.set(entity, now);

                Field updateUser = entity.getClass().getDeclaredField("updateUser");
                updateUser.setAccessible(true);
                updateUser.set(entity, currentId);
            }

            log.info("自动填充完成，实体类型: {}", entity.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("自动填充过程中发生异常", e);
        }
    }

}
