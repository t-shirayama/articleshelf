package com.readstack.adapter.web;

import com.readstack.application.article.TagResponse;
import com.readstack.application.article.TagService;
import com.readstack.application.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> findTags(@AuthenticationPrincipal CurrentUser user) {
        return tagService.findTags(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse addTag(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody TagRequest request) {
        return tagService.addTag(user, request.name());
    }

    @PatchMapping("/{id}")
    public TagResponse renameTag(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable UUID id,
            @Valid @RequestBody TagRequest request
    ) {
        return tagService.renameTag(user, id, request.name());
    }

    @PostMapping("/{sourceId}/merge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mergeTag(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable UUID sourceId,
            @Valid @RequestBody TagMergeRequest request
    ) {
        tagService.mergeTags(user, sourceId, request.targetTagId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnusedTag(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        tagService.deleteUnusedTag(user, id);
    }

    public record TagRequest(@NotBlank String name) {
    }

    public record TagMergeRequest(@NotNull UUID targetTagId) {
    }
}
