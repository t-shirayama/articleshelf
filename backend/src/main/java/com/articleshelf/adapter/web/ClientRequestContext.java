package com.articleshelf.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientRequestContext {
    private final ClientIpResolver clientIpResolver;

    public ClientRequestContext(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    public String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    public String ipAddress(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
