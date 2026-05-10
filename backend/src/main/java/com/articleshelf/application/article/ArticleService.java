package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.observability.BackendMetrics;
import com.articleshelf.domain.article.Article;
import com.articleshelf.domain.article.ArticleNotFoundException;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleSearchCriteria;
import com.articleshelf.domain.article.ArticleStatus;
import com.articleshelf.domain.article.ArticleUrlUnavailableException;
import com.articleshelf.domain.article.DuplicateArticleUrlException;
import com.articleshelf.domain.article.Tag;
import com.articleshelf.domain.article.TagName;
import com.articleshelf.domain.article.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final ArticleMetadataProvider metadataProvider;
    private final TransactionOperations transactionOperations;
    private final BackendMetrics metrics;

    public ArticleService(
            ArticleRepository articleRepository,
            TagRepository tagRepository,
            ArticleMetadataProvider metadataProvider,
            TransactionOperations transactionOperations,
            BackendMetrics metrics
    ) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.metadataProvider = metadataProvider;
        this.transactionOperations = transactionOperations;
        this.metrics = metrics;
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

    public ArticleResponse addArticle(CurrentUser user, AddArticleCommand command) {
        validateUniqueUrl(user, command.url(), null);

        ArticleMetadata metadata = metadataProvider.fetch(command.url());
        if (!metadata.accessible()) {
            throw new ArticleUrlUnavailableException(command.url());
        }

        return Objects.requireNonNull(transactionOperations.execute(status -> {
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

            ArticleResponse response = ArticleResponse.from(articleRepository.save(article));
            metrics.recordArticleCreated();
            return response;
        }));
    }

    public ArticleResponse updateArticle(CurrentUser user, UUID id, UpdateArticleCommand command) {
        Article current = loadArticleAndValidateUrl(user, id, command.url());
        boolean shouldRefreshMetadata = !command.url().equals(current.getUrl()) || current.getThumbnailUrl().isBlank();
        ArticleMetadata metadata = shouldRefreshMetadata ? metadataProvider.fetch(command.url()) : ArticleMetadata.empty();
        if (!metadata.accessible() && !command.url().equals(current.getUrl())) {
            throw new ArticleUrlUnavailableException(command.url());
        }

        return Objects.requireNonNull(transactionOperations.execute(status -> {
            current.updateContent(
                    command.url(),
                    firstPresent(command.title(), current.getTitle()),
                    command.summary(),
                    command.notes()
            );
            current.changeThumbnailUrl(firstPresent(metadata.imageUrl(), current.getThumbnailUrl()));
            current.changeStatus(command.status() == null ? current.getStatus() : command.status(), command.readDate());
            current.changeFavorite(command.favorite() == null ? current.isFavorite() : command.favorite());
            current.changeRating(command.rating() == null ? current.getRating() : command.rating());
            current.replaceTags(resolveTags(user, command.tags()));

            ArticleResponse response = ArticleResponse.from(articleRepository.save(current));
            metrics.recordArticleUpdated();
            return response;
        }));
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
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .forEach(name -> tags.add(tagRepository.saveTag(user.id(), TagName.normalize(name))));
        return tags;
    }

    private Article loadArticleAndValidateUrl(CurrentUser user, UUID id, String url) {
        return Objects.requireNonNull(transactionOperations.execute(status -> {
            Article current = articleRepository.findByIdAndUserId(id, user.id()).orElseThrow(() -> new ArticleNotFoundException(id));
            validateUniqueUrl(user, url, id);
            return current;
        }));
    }

    private void validateUniqueUrl(CurrentUser user, String url, UUID currentArticleId) {
        transactionOperations.executeWithoutResult(status -> articleRepository.findByUrlAndUserId(url, user.id())
                .filter(article -> currentArticleId == null || !article.getId().equals(currentArticleId))
                .ifPresent(article -> {
                    throw new DuplicateArticleUrlException(url, article.getId());
                }));
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
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
