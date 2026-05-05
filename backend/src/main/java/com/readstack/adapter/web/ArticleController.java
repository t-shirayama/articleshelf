package com.readstack.adapter.web;

import com.readstack.application.article.AddArticleCommand;
import com.readstack.application.article.ArticleResponse;
import com.readstack.application.article.ArticleService;
import com.readstack.application.article.UpdateArticleCommand;
import com.readstack.domain.article.ArticleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.http.HttpStatus;
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

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public List<ArticleResponse> findArticles(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean favorite
    ) {
        return articleService.findArticles(status, tag, search, favorite);
    }

    @GetMapping("/{id}")
    public ArticleResponse findArticle(@PathVariable UUID id) {
        return articleService.findArticle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse addArticle(@Valid @RequestBody ArticleRequest request) {
        return articleService.addArticle(request.toAddCommand());
    }

    @PutMapping("/{id}")
    public ArticleResponse updateArticle(@PathVariable UUID id, @Valid @RequestBody ArticleRequest request) {
        return articleService.updateArticle(id, request.toUpdateCommand());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable UUID id) {
        articleService.deleteArticle(id);
    }

    public record ArticleRequest(
            @NotBlank @URL String url,
            String title,
            String summary,
            ArticleStatus status,
            LocalDate readDate,
            Boolean favorite,
            String notes,
            List<String> tags
    ) {
        AddArticleCommand toAddCommand() {
            return new AddArticleCommand(url, title, summary, status, readDate, favorite, notes, tags);
        }

        UpdateArticleCommand toUpdateCommand() {
            return new UpdateArticleCommand(url, title, summary, status, readDate, favorite, notes, tags);
        }
    }
}
