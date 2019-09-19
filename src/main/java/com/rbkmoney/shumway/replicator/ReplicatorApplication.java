package com.rbkmoney.shumway.replicator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Created by vpankrashkin on 16.05.18.
 */
@EnableRetry
@SpringBootApplication(scanBasePackages = {"com.rbkmoney.shumway.replicator"})
public class ReplicatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReplicatorApplication.class, args);
    }
}