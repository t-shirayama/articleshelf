package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleNotFoundException;
import com.articleshelf.domain.article.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteArticleUseCase {
    private final ArticleRepository articleRepository;

    public DeleteArticleUseCase(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional
    public void deleteArticle(CurrentUser user, UUID id) {
        if (articleRepository.findByIdAndUserId(id, user.id()).isEmpty()) {
            throw new ArticleNotFoundException(id);
        }
        articleRepository.deleteByIdAndUserId(id, user.id());
    }
}
