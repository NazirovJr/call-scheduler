package com.callscheduler;

import java.time.Clock;
import java.time.ZoneOffset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CallSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallSchedulerApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.tickSeconds(ZoneOffset.UTC);
    }
}
