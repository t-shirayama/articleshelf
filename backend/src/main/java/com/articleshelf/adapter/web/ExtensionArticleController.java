package com.articleshelf.adapter.web;

import com.articleshelf.application.article.AddArticleCommand;
import com.articleshelf.application.article.ArticleResponse;
import com.articleshelf.application.article.ArticleService;
import com.articleshelf.application.article.TagResponse;
import com.articleshelf.application.article.UpdateArticleCommand;
import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/extension/articles")
@Validated
public class ExtensionArticleController {
    private final ArticleService articleService;

    public ExtensionArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/lookup")
    public ArticleResponse lookup(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam @NotBlank @URL @Size(max = 2048) String url
    ) {
        return articleService.findArticleByUrl(user, url);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse create(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody ExtensionArticleRequest request
    ) {
        return articleService.addArticle(user, new AddArticleCommand(
                request.url(),
                request.title(),
                null,
                request.status(),
                request.readDate(),
                false,
                0,
                null,
                List.of()
        ));
    }

    @PatchMapping("/{id}/status")
    public ArticleResponse updateStatus(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ExtensionStatusRequest request
    ) {
        ArticleResponse current = articleService.findArticle(user, id);
        return articleService.updateArticle(user, id, new UpdateArticleCommand(
                current.version(),
                current.url(),
                current.title(),
                current.summary(),
                request.status(),
                request.readDate(),
                current.favorite(),
                current.rating(),
                current.notes(),
                current.tags().stream().map(TagResponse::name).toList()
        ));
    }

    public record ExtensionArticleRequest(
            @NotBlank @URL @Size(max = 2048) String url,
            @Size(max = 255) String title,
            @NotNull ArticleStatus status,
            LocalDate readDate
    ) {
    }

    public record ExtensionStatusRequest(@NotNull ArticleStatus status, LocalDate readDate) {
    }
}
