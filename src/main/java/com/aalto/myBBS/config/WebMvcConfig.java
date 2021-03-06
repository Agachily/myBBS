package com.aalto.myBBS.config;

import com.aalto.myBBS.controller.interceptor.DataInterceptor;
import com.aalto.myBBS.controller.interceptor.LoginRequiredInterceptor;
import com.aalto.myBBS.controller.interceptor.LoginTicketInterceptor;
import com.aalto.myBBS.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    /* 重构项目，使用Spring Security来进行权限管理，因此将之前的拦截器废弃 */
    // @Autowired
    // private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    @Autowired
    private DataInterceptor dataInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.jpg","/**/*.jpeg", "/**/*.png");
        // registry.addInterceptor(loginRequiredInterceptor)
                // .excludePathPatterns("/**/*.css","/**/*.js","/**/*.jpg","/**/*.jpeg", "/**/*.png");
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.jpg","/**/*.jpeg", "/**/*.png");

        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }


}
