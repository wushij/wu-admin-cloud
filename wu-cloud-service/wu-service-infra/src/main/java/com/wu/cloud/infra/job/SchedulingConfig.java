package com.wu.cloud.infra.job;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务配置（P1-3 修复）
 * <p>
 * Feign 客户端统一在 {@link com.wu.cloud.infra.InfraApplication} 声明，
 * 此处勿再扫描 {@code com.wu.cloud.api}，否则会重复注册 Feign Bean，
 * 甚至把 {@code RemoteOnlineUserService}（指向本服务）注册成客户端导致启动失败。
 * </p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 定时任务线程池：供 DB 任务（sys_job）按 Cron 调度执行
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("wu-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
