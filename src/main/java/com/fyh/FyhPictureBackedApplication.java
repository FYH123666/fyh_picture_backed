package com.fyh;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.fyh.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)

public class FyhPictureBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(FyhPictureBackedApplication.class, args);
    }

}
