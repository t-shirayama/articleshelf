package com.articleshelf.adapter.web;

import com.articleshelf.domain.article.ArticleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.List;

public record UpdateArticleRequest(
        @NotNull @Min(0) Long version,
        @NotBlank @URL @Size(max = 2048) String url,
        @Size(max = 255) String title,
        @Size(max = 5000) String summary,
        ArticleStatus status,
        LocalDate readDate,
        Boolean favorite,
        @Min(0) @Max(5) Integer rating,
        @Size(max = 20000) String notes,
        @Size(max = 20) List<@Size(max = 255) String> tags
) {
}
