package com.recplatform.web;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.recplatform")
@EnableScheduling
@MapperScan(basePackages = "com.recplatform", markerInterface = BaseMapper.class)
public class RecPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecPlatformApplication.class, args);
    }
}
