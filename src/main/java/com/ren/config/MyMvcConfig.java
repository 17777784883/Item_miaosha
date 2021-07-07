package com.ren.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Ren
 * 自定义的MVC配置类
 */




@Configuration  //这个注解 让这个类变成一个配置类
// 类型必须为WebMvcConfigurer  所以要实现WebMvcConfigurer接口
public class MyMvcConfig implements WebMvcConfigurer{


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("==========静态资源拦截！============");
        //将所有/static/** 访问都映射到classpath:/static/ 目录下
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

}
