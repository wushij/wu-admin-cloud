package com.wu.cloud.api.log;

import cn.hutool.json.JSONUtil;
import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.api.system.feign.RemoteLogService;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.utils.web.ClientIpUtils;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 操作日志切面（对齐单体：支持 {@link OperLogContext} 跳过/自定义标题/摘要/变更明细）。
 *
 * <p>拦截所有标注 {@link Log} 的方法，采集请求/响应/异常信息后写入 system {@code sys_oper_log}。</p>
 *
 * <p>异步上报：有界队列 + 独立线程池，避免 system 抖动反压业务线程。</p>
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
                        log.warn("[OperLog] 上报队列已满，已累计丢弃 {} 条操作日志（system 可能不可用）", n);
                    }
                });
    }

    @Before("@annotation(com.wu.cloud.api.log.Log)")
    public void doBefore() {
        START_TIME.set(System.currentTimeMillis());
        OperLogContext.clear();
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
            if (OperLogContext.isSkipLog()) {
                return;
            }
            Method method = resolveMethod(joinPoint);
            Log annotation = AnnotationUtils.findAnnotation(method, Log.class);
            if (annotation == null) {
                return;
            }
            Long start = START_TIME.get();
            long cost = start != null ? System.currentTimeMillis() - start : 0L;

            String customTitle = OperLogContext.getTitle();
            String customAction = OperLogContext.getAction();
            List<String> diffItems = OperLogContext.getDiffItems();

            OperLogDTO dto = new OperLogDTO();
            String title = annotation.title().isEmpty() ? annotation.value() : annotation.title();
            if (customTitle != null && !customTitle.isBlank()) {
                title = customTitle;
            }
            dto.setTitle(title);
            dto.setBusinessType(annotation.businessType().getType());
            dto.setOperatorType(annotation.operatorType().getType());
            dto.setMethod(joinPoint.getSignature().getDeclaringType().getName() + "." + method.getName());
            dto.setCostTime(cost);
            dto.setOperTime(LocalDateTime.now());
            dto.setStatus(e == null ? 0 : 1);
            dto.setErrorMsg(e != null ? truncate(e.getMessage(), MAX_RESULT_LEN) : null);

            // ThreadLocal 上下文必须在业务线程内取出
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                dto.setRequestMethod(request.getMethod());
                dto.setOperUrl(request.getRequestURI());
                dto.setOperIp(ClientIpUtils.resolve(request));
                if (annotation.isSaveRequestData()) {
                    dto.setOperParam(buildParamJson(joinPoint, customAction, diffItems));
                }
            }
            dto.setOperName(UserContextHolder.getUsername());

            if (annotation.isSaveResponseData() && result != null) {
                String json = JSONUtil.toJsonStr(result);
                dto.setJsonResult(truncate(json, MAX_RESULT_LEN));
            }

            submit(dto);
        } catch (Exception ignored) {
            // 切面异常不能影响业务
        } finally {
            START_TIME.remove();
            OperLogContext.clear();
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

    private String buildParamJson(JoinPoint joinPoint, String customAction, List<String> diffItems) {
        try {
            Object[] args = joinPoint.getArgs();
            List<Object> validArgs = new ArrayList<>();
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null && !isFilterObject(arg)) {
                        validArgs.add(arg);
                    }
                }
            }

            Map<String, Object> reqData = new LinkedHashMap<>();
            if (customAction != null && !customAction.isBlank()) {
                reqData.put("action", customAction);
            }
            if (diffItems != null && !diffItems.isEmpty()) {
                reqData.put("diffItems", diffItems);
            }

            if (!reqData.isEmpty()) {
                if (validArgs.size() == 1) {
                    reqData.put("params", validArgs.get(0));
                } else if (!validArgs.isEmpty()) {
                    reqData.put("params", validArgs);
                }
                return truncate(maskSensitive(JSONUtil.toJsonStr(reqData)), MAX_RESULT_LEN);
            }

            if (validArgs.isEmpty()) {
                return null;
            }
            return truncate(maskSensitive(JSONUtil.toJsonStr(validArgs.size() == 1 ? validArgs.get(0) : validArgs)),
                    MAX_RESULT_LEN);
        } catch (Exception e) {
            return null;
        }
    }

    private String maskSensitive(String json) {
        if (json == null) {
            return null;
        }
        return json.replaceAll("(\"password\"\\s*:\\s*)\"[^\"]*\"", "$1\"******\"");
    }

    private boolean isFilterObject(Object obj) {
        if (obj instanceof MultipartFile || obj instanceof HttpServletRequest || obj instanceof HttpServletResponse) {
            return true;
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return MultipartFile.class.isAssignableFrom(clazz.getComponentType());
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            for (Object item : (Collection<?>) obj) {
                if (item instanceof MultipartFile) {
                    return true;
                }
            }
        }
        if (Map.class.isAssignableFrom(clazz)) {
            for (Object value : ((Map<?, ?>) obj).values()) {
                if (value instanceof MultipartFile) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private Method resolveMethod(JoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return joinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());
    }
}
