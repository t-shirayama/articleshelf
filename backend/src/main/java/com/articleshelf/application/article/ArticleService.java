package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ArticleService {
    private final SearchArticlesQuery searchArticlesQuery;
    private final FindArticleQuery findArticleQuery;
    private final AddArticleUseCase addArticleUseCase;
    private final UpdateArticleUseCase updateArticleUseCase;
    private final DeleteArticleUseCase deleteArticleUseCase;

    public ArticleService(
            SearchArticlesQuery searchArticlesQuery,
            FindArticleQuery findArticleQuery,
            AddArticleUseCase addArticleUseCase,
            UpdateArticleUseCase updateArticleUseCase,
            DeleteArticleUseCase deleteArticleUseCase
    ) {
        this.searchArticlesQuery = searchArticlesQuery;
        this.findArticleQuery = findArticleQuery;
        this.addArticleUseCase = addArticleUseCase;
        this.updateArticleUseCase = updateArticleUseCase;
        this.deleteArticleUseCase = deleteArticleUseCase;
    }

    public List<ArticleResponse> findArticles(CurrentUser user, ArticleStatus status, String tag, String search, Boolean favorite) {
        return searchArticlesQuery.findArticles(user, status, tag, search, favorite);
    }

    public ArticleResponse findArticle(CurrentUser user, UUID id) {
        return findArticleQuery.findArticle(user, id);
    }

    public ArticleResponse addArticle(CurrentUser user, AddArticleCommand command) {
        return addArticleUseCase.addArticle(user, command);
    }

    public ArticleResponse updateArticle(CurrentUser user, UUID id, UpdateArticleCommand command) {
        return updateArticleUseCase.updateArticle(user, id, command);
    }

    public void deleteArticle(CurrentUser user, UUID id) {
        deleteArticleUseCase.deleteArticle(user, id);
    }
}
