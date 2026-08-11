package com.wu.cloud.api.log;

import cn.hutool.json.JSONUtil;
import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.api.system.feign.RemoteLogService;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.utils.web.ClientIpUtils;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 操作日志切面。
 *
 * <p>拦截所有标注 {@link Log} 的方法，采集请求/响应/异常信息后写入 infra 操作日志。</p>
 *
 * <p><b>P1-3 修复</b>：原实现虽然注释写着「异步写入」，但实际是在业务线程里
 * <b>同步</b>调用 {@code remote.saveOperLog(dto)}。这带来两个问题：</p>
 * <ol>
 *   <li>每个带 @Log 的请求都要多等一次跨服务 RPC，直接抬高接口 RT；</li>
 *   <li>infra 服务抖动/变慢时会反压所有业务服务的工作线程，可能引发级联故障
 *       （日志是旁路能力，绝不应拖垮主链路）。</li>
 * </ol>
 *
 * <p>现改为投递到<b>有界队列 + 独立线程池</b>异步上报：</p>
 * <ul>
 *   <li>有界队列（4096）避免 infra 长时间不可用时内存无限增长；</li>
 *   <li>拒绝策略为 DiscardOldest 并计数告警——日志可丢，业务不可挂；</li>
 *   <li>上报线程为守护线程，关闭时优雅等待少量时间冲刷剩余日志。</li>
 * </ul>
 *
 * <p>注意：DTO 在业务线程内组装完毕后才入队，因为 {@link UserContextHolder}
 * 与 {@link RequestContextHolder} 都是 ThreadLocal，异步线程中取不到。</p>
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /** 响应体最大留存长度，防止大响应撑爆日志表 */
    private static final int MAX_RESULT_LEN = 2000;
    /** 异步队列容量 */
    private static final int QUEUE_CAPACITY = 4096;

    private final ObjectProvider<RemoteLogService> remoteLogServiceProvider;

    /** 每个线程的临时上下文（记录开始时间） */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /** 被丢弃的日志条数（用于观测积压） */
    private final AtomicLong discarded = new AtomicLong();

    /** 日志上报线程池：单线程 + 有界队列，最大限度降低对业务的影响 */
    private final ThreadPoolExecutor reporter;

    public LogAspect(ObjectProvider<RemoteLogService> remoteLogServiceProvider) {
        this.remoteLogServiceProvider = remoteLogServiceProvider;
        this.reporter = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "oper-log-reporter");
                    t.setDaemon(true);
                    return t;
                },
                (r, executor) -> {
                    long n = discarded.incrementAndGet();
                    if (n % 100 == 1) {
                        log.warn("[OperLog] 上报队列已满，已累计丢弃 {} 条操作日志（infra 可能不可用）", n);
                    }
                });
    }

    @Before("@annotation(com.wu.cloud.api.log.Log)")
    public void doBefore() {
        START_TIME.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(com.wu.cloud.api.log.Log)", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, result, null);
    }

    @AfterThrowing(pointcut = "@annotation(com.wu.cloud.api.log.Log)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, null, e);
    }

    private void handleLog(JoinPoint joinPoint, Object result, Exception e) {
        try {
            Method method = resolveMethod(joinPoint);
            Log annotation = AnnotationUtils.findAnnotation(method, Log.class);
            if (annotation == null) {
                return;
            }
            Long start = START_TIME.get();
            long cost = start != null ? System.currentTimeMillis() - start : 0L;

            OperLogDTO dto = new OperLogDTO();
            dto.setTitle(annotation.title().isEmpty() ? annotation.value() : annotation.title());
            dto.setBusinessType(annotation.businessType().getType());
            dto.setOperatorType(annotation.operatorType().getType());
            dto.setMethod(joinPoint.getSignature().getDeclaringType().getName() + "." + method.getName());
            dto.setCostTime(cost);
            dto.setOperTime(LocalDateTime.now());
            dto.setStatus(e == null ? 0 : 1);
            dto.setErrorMsg(e != null ? e.getMessage() : null);

            // ThreadLocal 上下文必须在业务线程内取出
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                dto.setRequestMethod(request.getMethod());
                dto.setOperUrl(request.getRequestURI());
                dto.setOperIp(ClientIpUtils.resolve(request));
                if (annotation.isSaveRequestData()) {
                    dto.setOperParam(buildParamJson(joinPoint));
                }
            }
            dto.setOperName(UserContextHolder.getUsername());

            if (annotation.isSaveResponseData() && result != null) {
                String json = JSONUtil.toJsonStr(result);
                if (json != null && json.length() > MAX_RESULT_LEN) {
                    json = json.substring(0, MAX_RESULT_LEN);
                }
                dto.setJsonResult(json);
            }

            submit(dto);
        } catch (Exception ignored) {
            // 切面异常不能影响业务
        } finally {
            START_TIME.remove();
        }
    }

    /**
     * 异步投递日志，不阻塞业务线程。
     */
    private void submit(OperLogDTO dto) {
        reporter.execute(() -> {
            try {
                RemoteLogService remote = remoteLogServiceProvider.getIfAvailable();
                if (remote != null) {
                    remote.saveOperLog(dto);
                }
            } catch (Exception ex) {
                // 日志写入失败不影响主流程，仅 debug 记录避免刷屏
                log.debug("[OperLog] 上报失败: {}", ex.getMessage());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        reporter.shutdown();
        try {
            if (!reporter.awaitTermination(5, TimeUnit.SECONDS)) {
                reporter.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            reporter.shutdownNow();
        }
    }

    private String buildParamJson(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            return JSONUtil.toJsonStr(args);
        } catch (Exception e) {
            return null;
        }
    }

    private Method resolveMethod(JoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return joinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());
    }
}
