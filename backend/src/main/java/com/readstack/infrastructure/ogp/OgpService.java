package com.readstack.infrastructure.ogp;

import org.springframework.stereotype.Service;

@Service
public class OgpService {
    private final OgpClient ogpClient;

    public OgpService(OgpClient ogpClient) {
        this.ogpClient = ogpClient;
    }

    public OgpMetadata fetch(String url) {
        if (url == null || url.isBlank()) {
            return OgpMetadata.unavailable();
        }
        return ogpClient.fetch(url);
    }
}
