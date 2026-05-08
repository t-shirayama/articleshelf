package com.articleshelf.infrastructure.ogp;

public record OgpMetadata(String title, String description, String imageUrl, boolean accessible) {
    public static OgpMetadata empty() {
        return new OgpMetadata("", "", "", true);
    }

    public static OgpMetadata unavailable() {
        return new OgpMetadata("", "", "", false);
    }
}
