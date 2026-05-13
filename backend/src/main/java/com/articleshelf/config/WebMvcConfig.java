package com.articleshelf.config;

import com.articleshelf.adapter.web.CookieCsrfGuardInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final CookieCsrfGuardInterceptor cookieCsrfGuardInterceptor;

    public WebMvcConfig(CookieCsrfGuardInterceptor cookieCsrfGuardInterceptor) {
        this.cookieCsrfGuardInterceptor = cookieCsrfGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cookieCsrfGuardInterceptor);
    }
}
