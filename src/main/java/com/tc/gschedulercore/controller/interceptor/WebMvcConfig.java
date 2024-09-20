package com.tc.gschedulercore.controller.interceptor;

import com.tc.gschedulercore.controller.auth.AuthorizationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * web mvc config
 *
 * @author honggang.liu 2018-04-02 20:48:20
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private CookieInterceptor cookieInterceptor;

    /**
     * 允许http类型
     */
    private static final String ORIGINS[] = new String[]{"GET", "POST", "PUT", "DELETE"};


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cookieInterceptor).addPathPatterns("/**");
        registry.addInterceptor(authorizationInterceptor()).excludePathPatterns("/static/**", "/api/**", "/registry/**", "/jobinfo/openapi/**", "/joblog/openapi/**");
    }

    @Bean
    public AuthorizationInterceptor authorizationInterceptor() {
        return new AuthorizationInterceptor();
    }

    /**
     * 配置静态资源访问路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源访问路径和存放路径配置
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .setCachePeriod(604800).resourceChain(true);
        // swagger访问配置
        //排除静态文件
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods(ORIGINS)
                .allowCredentials(true)
                .maxAge(3600);
    }

}