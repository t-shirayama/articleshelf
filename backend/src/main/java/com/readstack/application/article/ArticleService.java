package com.readstack.application.article;

import com.readstack.application.auth.CurrentUser;
import com.readstack.domain.article.Article;
import com.readstack.domain.article.ArticleNotFoundException;
import com.readstack.domain.article.ArticleRepository;
import com.readstack.domain.article.ArticleStatus;
import com.readstack.domain.article.ArticleUrlUnavailableException;
import com.readstack.domain.article.DuplicateArticleUrlException;
import com.readstack.domain.article.Tag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleMetadataProvider metadataProvider;

    public ArticleService(ArticleRepository articleRepository, ArticleMetadataProvider metadataProvider) {
        this.articleRepository = articleRepository;
        this.metadataProvider = metadataProvider;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findArticles(CurrentUser user, ArticleStatus status, String tag, String search, Boolean favorite) {
        String normalizedTag = normalizeFilter(tag);
        String normalizedSearch = normalizeFilter(search);

        return articleRepository.findAllByUserId(user.id()).stream()
                .filter(article -> status == null || article.getStatus() == status)
                .filter(article -> favorite == null || article.isFavorite() == favorite)
                .filter(article -> normalizedTag == null || article.getTags().stream()
                        .anyMatch(item -> item.getName().equalsIgnoreCase(normalizedTag)))
                .filter(article -> normalizedSearch == null || matchesSearch(article, normalizedSearch))
                .sorted(Comparator.comparing(Article::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
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

    @Transactional(readOnly = true)
    public List<TagResponse> findTags(CurrentUser user) {
        return articleRepository.findAllTagsByUserId(user.id()).stream()
                .map(TagResponse::from)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList();
    }

    @Transactional
    public TagResponse addTag(CurrentUser user, String name) {
        return TagResponse.from(articleRepository.saveTag(user.id(), name));
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
                .forEach(name -> tags.add(articleRepository.saveTag(user.id(), name)));
        return tags;
    }

    private boolean matchesSearch(Article article, String search) {
        String haystack = String.join(" ",
                nullToEmpty(article.getTitle()),
                nullToEmpty(article.getUrl()),
                nullToEmpty(article.getSummary()),
                nullToEmpty(article.getNotes())
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(search);
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
