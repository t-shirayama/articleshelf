package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleNotFoundException;
import com.articleshelf.domain.article.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindArticleQuery {
    private final ArticleRepository articleRepository;

    public FindArticleQuery(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public ArticleResponse findArticle(CurrentUser user, UUID id) {
        return articleRepository.findByIdAndUserId(id, user.id())
                .map(ArticleResponse::from)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }
}
