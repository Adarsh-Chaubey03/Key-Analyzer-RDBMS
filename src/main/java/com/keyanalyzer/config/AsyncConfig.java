package com.keyanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AsyncConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService requestTimeoutExecutor() {
        int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        AtomicInteger threadCounter = new AtomicInteger();

        return Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("key-analyzer-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
