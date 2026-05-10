package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleSearchCriteria;
import com.articleshelf.domain.article.ArticleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            String tag,
            String search,
            Boolean favorite,
            ArticleListQuery query
    ) {
        ArticleSearchCriteria criteria = new ArticleSearchCriteria(status, normalizeFilter(tag), normalizeFilter(search), favorite);
        return query.slice(articleRepository.searchByUserId(user.id(), criteria)).stream()
                .map(ArticleResponse::from)
                .toList();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
