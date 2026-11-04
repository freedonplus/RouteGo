package com.rg.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 控制器日志切面
 * 自动打印每个 Controller 方法的入参、耗时、返回结果
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    private static final String SEPARATOR = "----------------------------------------------------------------------------------------------------------";

    @Pointcut("execution(* com.rg..controller..*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        System.out.println();
        System.out.println("================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================");
        log.info(SEPARATOR);
        log.info(">> {}.{} 调用, 参数: {}", className, methodName, Arrays.toString(args));

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("<< {}.{} 返回, 耗时: {}ms, 结果: {}", className, methodName, cost, result);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.error("<< {}.{} 异常, 耗时: {}ms, 异常: {}", className, methodName, cost, e.getMessage());
            throw e;
        } finally {
            log.info(SEPARATOR);
            System.out.println("================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================");
        }
    }
}
