package com.ancientbooks;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 古籍分析 Agent 后端启动类
 */
@SpringBootApplication
@MapperScan("com.ancientbooks.mapper")
public class AnalyseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyseApplication.class, args);
    }

}
