package com.readstack.application.article;

import com.readstack.application.auth.CurrentUser;
import com.readstack.domain.article.Article;
import com.readstack.domain.article.ArticleNotFoundException;
import com.readstack.domain.article.ArticleRepository;
import com.readstack.domain.article.ArticleSearchCriteria;
import com.readstack.domain.article.ArticleStatus;
import com.readstack.domain.article.ArticleUrlUnavailableException;
import com.readstack.domain.article.DuplicateArticleUrlException;
import com.readstack.domain.article.Tag;
import com.readstack.domain.article.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final ArticleMetadataProvider metadataProvider;

    public ArticleService(ArticleRepository articleRepository, TagRepository tagRepository, ArticleMetadataProvider metadataProvider) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.metadataProvider = metadataProvider;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findArticles(CurrentUser user, ArticleStatus status, String tag, String search, Boolean favorite) {
        ArticleSearchCriteria criteria = new ArticleSearchCriteria(status, normalizeFilter(tag), normalizeFilter(search), favorite);
        return articleRepository.searchByUserId(user.id(), criteria).stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse findArticle(CurrentUser user, UUID id) {
        return articleRepository.findByIdAndUserId(id, user.id())
                .map(ArticleResponse::from)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }

    @Transactional
    public ArticleResponse addArticle(CurrentUser user, AddArticleCommand command) {
        articleRepository.findByUrlAndUserId(command.url(), user.id())
                .ifPresent(article -> {
                    throw new DuplicateArticleUrlException(command.url(), article.getId());
                });

        ArticleMetadata metadata = metadataProvider.fetch(command.url());
        if (!metadata.accessible()) {
            throw new ArticleUrlUnavailableException(command.url());
        }
        Article article = new Article(
                null,
                user.id(),
                command.url(),
                firstPresent(command.title(), metadata.title(), command.url()),
                firstPresent(command.summary(), metadata.description(), ""),
                metadata.imageUrl(),
                command.status(),
                command.readDate(),
                Boolean.TRUE.equals(command.favorite()),
                command.rating() == null ? 0 : command.rating(),
                command.notes(),
                resolveTags(user, command.tags()),
                null,
                null
        );

        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse updateArticle(CurrentUser user, UUID id, UpdateArticleCommand command) {
        Article current = articleRepository.findByIdAndUserId(id, user.id()).orElseThrow(() -> new ArticleNotFoundException(id));
        articleRepository.findByUrlAndUserId(command.url(), user.id())
                .filter(article -> !article.getId().equals(id))
                .ifPresent(article -> {
                    throw new DuplicateArticleUrlException(command.url(), article.getId());
                });
        boolean shouldRefreshMetadata = !command.url().equals(current.getUrl()) || current.getThumbnailUrl().isBlank();
        ArticleMetadata metadata = shouldRefreshMetadata ? metadataProvider.fetch(command.url()) : ArticleMetadata.empty();
        if (!metadata.accessible() && !command.url().equals(current.getUrl())) {
            throw new ArticleUrlUnavailableException(command.url());
        }

        Article updated = new Article(
                current.getId(),
                current.getUserId(),
                command.url(),
                firstPresent(command.title(), current.getTitle()),
                command.summary(),
                firstPresent(metadata.imageUrl(), current.getThumbnailUrl()),
                command.status() == null ? current.getStatus() : command.status(),
                command.readDate(),
                command.favorite() == null ? current.isFavorite() : command.favorite(),
                command.rating() == null ? current.getRating() : command.rating(),
                command.notes(),
                resolveTags(user, command.tags()),
                current.getCreatedAt(),
                current.getUpdatedAt()
        );

        return ArticleResponse.from(articleRepository.save(updated));
    }

    @Transactional
    public void deleteArticle(CurrentUser user, UUID id) {
        if (articleRepository.findByIdAndUserId(id, user.id()).isEmpty()) {
            throw new ArticleNotFoundException(id);
        }
        articleRepository.deleteByIdAndUserId(id, user.id());
    }

    private Set<Tag> resolveTags(CurrentUser user, List<String> names) {
        if (names == null) {
            return new LinkedHashSet<>();
        }

        Set<Tag> tags = new LinkedHashSet<>();
        names.stream()
                .map(this::normalizeName)
                .filter(value -> !value.isBlank())
                .distinct()
                .forEach(name -> tags.add(tagRepository.saveTag(user.id(), name)));
        return tags;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

}
