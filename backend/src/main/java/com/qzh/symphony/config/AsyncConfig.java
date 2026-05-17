package com.qzh.symphony.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);     //核心线程数为10线程数,待命
        executor.setMaxPoolSize(50);      //最大线程数为50线程数
        executor.setQueueCapacity(100);   //队列容量,最大100个任务
        executor.setThreadNamePrefix("ai-stream-");
        executor.initialize();
        executor.setWaitForTasksToCompleteOnShutdown(true); //应用关闭时等任务跑完
        executor.setAwaitTerminationSeconds(120);           //最多等2分钟
        return executor;
    }
}