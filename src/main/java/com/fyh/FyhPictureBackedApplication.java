package com.fyh;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.fyh.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class FyhPictureBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(FyhPictureBackedApplication.class, args);
    }

}
