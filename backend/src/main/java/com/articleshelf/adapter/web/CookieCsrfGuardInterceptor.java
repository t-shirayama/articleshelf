package com.articleshelf.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CookieCsrfGuardInterceptor implements HandlerInterceptor {
    private final CsrfTokenValidator csrfTokenValidator;

    public CookieCsrfGuardInterceptor(CsrfTokenValidator csrfTokenValidator) {
        this.csrfTokenValidator = csrfTokenValidator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!requiresCsrf(handlerMethod)) {
            return true;
        }
        csrfTokenValidator.validate(request);
        return true;
    }

    private boolean requiresCsrf(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(CookieCsrfProtected.class)
                || handlerMethod.getBeanType().isAnnotationPresent(CookieCsrfProtected.class);
    }
}
