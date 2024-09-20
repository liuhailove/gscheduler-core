package com.tc.gschedulercore;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 监控切面
 *
 * @author honggang.liu
 */
@Component
@Aspect
public class PromethuesAspectAop {

    /**
     * 监控相关
     */
    @Resource
    private MeterRegistry registry;

    private Counter counterTotal;

    ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(public * com.tc.gschedulercore.controller.*.*(..))")
    private void pointCut() {
    }

    @PostConstruct
    public void init() {
        counterTotal = registry.counter("app_requests_count", "v1", "core");
    }

    @Before("pointCut()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        startTime.set(System.currentTimeMillis());
        counterTotal.increment();
    }

    @AfterReturning(returning = "returnVal", pointcut = "pointCut()")
    public void doAftereReturning(Object returnVal) {
        //System.out.println("请求执行时间：" + (System.currentTimeMillis() - startTime.get()));
    }
}
