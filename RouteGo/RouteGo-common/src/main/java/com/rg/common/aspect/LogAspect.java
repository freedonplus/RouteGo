package com.rg.common.aspect;

import com.rg.common.log.LogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 控制器日志切面：打印脱敏后的入参、耗时与返回结果。
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    private static final int MAX_TEXT_LENGTH = 1000;

    @Pointcut("execution(* com.rg..controller..*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.debug(">> {}.{} 参数: {}", className, methodName,
                LogSanitizer.toJson(joinPoint.getArgs(), MAX_TEXT_LENGTH));

        try {
            Object result = joinPoint.proceed();
            log.debug("<< {}.{} 返回, 耗时: {}ms, 结果: {}", className, methodName,
                    System.currentTimeMillis() - start,
                    LogSanitizer.toJson(result, MAX_TEXT_LENGTH));
            return result;
        } catch (Throwable e) {
            log.error("<< {}.{} 异常, 耗时: {}ms", className, methodName,
                    System.currentTimeMillis() - start, e);
            throw e;
        }
    }
}
