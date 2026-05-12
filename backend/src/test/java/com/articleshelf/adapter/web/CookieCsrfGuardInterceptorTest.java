package com.articleshelf.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CookieCsrfGuardInterceptorTest {
    @Test
    void validatesAnnotatedHandlers() throws Exception {
        HandlerInterceptor interceptor = new CookieCsrfGuardInterceptor(new CsrfTokenValidator(new TestAuthSessionSettings(true)));
        HandlerMethod handlerMethod = handlerMethod("protectedAction");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.setCookies(new jakarta.servlet.http.Cookie(SessionCookieWriter.CSRF_COOKIE, "token"));
        request.addHeader("X-CSRF-Token", "token");

        assertThatCode(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod))
                .doesNotThrowAnyException();
    }

    @Test
    void skipsNonAnnotatedHandlers() throws Exception {
        HandlerInterceptor interceptor = new CookieCsrfGuardInterceptor(new CsrfTokenValidator(new TestAuthSessionSettings(true)));
        HandlerMethod handlerMethod = handlerMethod("plainAction");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/articles");

        assertThatCode(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnnotatedHandlersWithMissingToken() throws Exception {
        HandlerInterceptor interceptor = new CookieCsrfGuardInterceptor(new CsrfTokenValidator(new TestAuthSessionSettings(true)));
        HandlerMethod handlerMethod = handlerMethod("protectedAction");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod))
                .isInstanceOf(CsrfValidationException.class);
    }

    private HandlerMethod handlerMethod(String methodName) throws Exception {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    private record TestAuthSessionSettings(boolean csrfEnabled) implements com.articleshelf.application.auth.AuthSessionSettings {
        @Override
        public long refreshTokenTtlDays() {
            return 30;
        }

        @Override
        public boolean cookieSecure() {
            return false;
        }

        @Override
        public String cookieSameSite() {
            return "Lax";
        }
    }

    @RestController
    private static class TestController {
        @PostMapping("/protected")
        @CookieCsrfProtected
        void protectedAction() {
        }

        @PostMapping("/plain")
        void plainAction() {
        }
    }
}
