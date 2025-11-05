package com.fyh.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring MVC Json 配置
 */

/*
    * 解决前端 Long 类型精度丢失问题
    * JavaScript 中的 Number 类型是基于 IEEE 754 双精度浮点数表示的，
    * 它能够精确表示的整数范围是 -2^53 到 2^53 之间的整数（即 -9007199254740992 到 9007199254740992）。
    * 当 Java 后端传递的 Long 类型数据超过这个范围时，JavaScript 在处理这些数字时会出现精度丢失的问题，
    * 导致前端接收到的数字不准确。
    * 为了解决这个问题，通常的做法是将 Long 类型的数据在序列化为 JSON 时转换为字符串类型，
    * 这样前端在接收数据时就不会因为数字精度问题而出错。
 */
@JsonComponent
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        return objectMapper;
    }
}
