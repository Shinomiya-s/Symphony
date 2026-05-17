package com.qzh.symphony;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.qzh.symphony.mapper")
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
public class SymphonyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SymphonyApplication.class, args);
    }

}
