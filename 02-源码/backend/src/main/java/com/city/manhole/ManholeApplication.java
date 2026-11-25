package com.city.manhole;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 城市井盖巡检与维修工单管理系统 启动类
 */
@SpringBootApplication
@MapperScan("com.city.manhole.mapper")
public class ManholeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManholeApplication.class, args);
    }
}
