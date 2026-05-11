package com.articleshelf.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ArticlePreviewRequest(
        @NotBlank @URL @Size(max = 2048) String url
) {
}
