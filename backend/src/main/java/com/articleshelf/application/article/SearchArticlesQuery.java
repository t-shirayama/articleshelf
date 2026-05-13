package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleListQuery;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleSearchCriteria;
import com.articleshelf.domain.article.ArticleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
public class SearchArticlesQuery {
    private final ArticleRepository articleRepository;

    public SearchArticlesQuery(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findArticles(CurrentUser user, ArticleStatus status, String tag, String search, Boolean favorite) {
        return findArticles(user, status, tag, search, favorite, new ArticleListQuery(null, null, null));
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findArticles(
            CurrentUser user,
            ArticleStatus status,
            List<String> tags,
            String search,
            Boolean favorite,
            List<Integer> ratings,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate readFrom,
            LocalDate readTo,
            ArticleListQuery query
    ) {
        ArticleSearchCriteria criteria = new ArticleSearchCriteria(
                status,
                normalizeFilters(tags),
                normalizeFilter(search),
                favorite,
                normalizeRatings(ratings),
                toCreatedFrom(createdFrom),
                toCreatedToExclusive(createdTo),
                readFrom,
                readTo
        );
        return articleRepository.searchByUserId(user.id(), criteria, query).stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findArticles(
            CurrentUser user,
            ArticleStatus status,
            String tag,
            String search,
            Boolean favorite,
            ArticleListQuery query
    ) {
        return findArticles(
                user,
                status,
                tag == null ? List.of() : List.of(tag),
                search,
                favorite,
                List.of(),
                null,
                null,
                null,
                null,
                query
        );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> normalizeFilters(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeFilter)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private List<Integer> normalizeRatings(List<Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return List.of();
        }
        return ratings.stream()
                .filter(rating -> rating != null && rating >= 0 && rating <= 5)
                .distinct()
                .sorted()
                .toList();
    }

    private Instant toCreatedFrom(LocalDate value) {
        return value == null ? null : value.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toCreatedToExclusive(LocalDate value) {
        return value == null ? null : value.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
