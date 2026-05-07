package com.readstack.adapter.web;

import com.readstack.application.article.ArticleService;
import com.readstack.application.article.TagResponse;
import com.readstack.application.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final ArticleService articleService;

    public TagController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public List<TagResponse> findTags(@AuthenticationPrincipal CurrentUser user) {
        return articleService.findTags(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse addTag(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody TagRequest request) {
        return articleService.addTag(user, request.name());
    }

    public record TagRequest(@NotBlank String name) {
    }
}
