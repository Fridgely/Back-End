package soon.fridgely.global.support.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class SchedulerLoggingAspect {

    private static final String TRACE_ID = "traceId";

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object bindSchedulerTraceId(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = "SCHED-" + UUID.randomUUID().toString().substring(0, 16);
        MDC.put(TRACE_ID, traceId);

        String methodName = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.info("[Scheduler] {} 시작", methodName);

        try {
            Object result = joinPoint.proceed();
            log.info("[Scheduler] {} 종료 ({}ms) - 결과: {}", methodName, System.currentTimeMillis() - start, result);
            return result;
        } catch (Exception e) {
            log.error("[Scheduler] {} 실패 ({}ms)", methodName, System.currentTimeMillis() - start, e);
            throw e;
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

}