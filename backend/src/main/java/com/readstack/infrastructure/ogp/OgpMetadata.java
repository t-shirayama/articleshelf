package com.readstack.infrastructure.ogp;

public record OgpMetadata(String title, String description, String imageUrl) {
    public static OgpMetadata empty() {
        return new OgpMetadata("", "", "");
    }
}
