package com.readstack.infrastructure.ogp;

public record OgpMetadata(String title, String description) {
    public static OgpMetadata empty() {
        return new OgpMetadata("", "");
    }
}
