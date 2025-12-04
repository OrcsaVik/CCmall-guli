package com.github.CCmall.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Vik
 * @date 2025-12-02
 * @description
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {


//
//    default void addViewControllers(ViewControllerRegistry registry) {
//    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/registers").setViewName("register");
    }

}
