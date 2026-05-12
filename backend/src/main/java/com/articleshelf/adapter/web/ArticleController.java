package com.articleshelf.adapter.web;

import com.articleshelf.application.article.ArticleResponse;
import com.articleshelf.application.article.ArticleService;
import com.articleshelf.application.article.ArticleListQuery;
import com.articleshelf.application.article.ArticlePreviewResponse;
import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final ArticleRequestMapper articleRequestMapper;

    public ArticleController(ArticleService articleService, ArticleRequestMapper articleRequestMapper) {
        this.articleService = articleService;
        this.articleRequestMapper = articleRequestMapper;
    }

    @GetMapping
    public List<ArticleResponse> findArticles(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) List<String> tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(required = false) List<Integer> rating,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(required = false) LocalDate readFrom,
            @RequestParam(required = false) LocalDate readTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return articleService.findArticles(
                user,
                status,
                tag,
                search,
                favorite,
                rating,
                createdFrom,
                createdTo,
                readFrom,
                readTo,
                new ArticleListQuery(page, size, sort)
        );
    }

    @GetMapping("/{id}")
    public ArticleResponse findArticle(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return articleService.findArticle(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse addArticle(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody ArticleRequest request) {
        return articleService.addArticle(user, articleRequestMapper.toAddCommand(request));
    }

    @PostMapping("/preview")
    public ArticlePreviewResponse previewArticle(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody ArticlePreviewRequest request
    ) {
        return articleService.previewArticle(user, request.url());
    }

    @PutMapping("/{id}")
    public ArticleResponse updateArticle(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateArticleRequest request
    ) {
        return articleService.updateArticle(user, id, articleRequestMapper.toUpdateCommand(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        articleService.deleteArticle(user, id);
    }
}
