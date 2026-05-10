package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.DuplicateArticleUrlException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;

@Service
public class ArticleUrlUniquenessGuard {
    private final ArticleRepository articleRepository;
    private final TransactionOperations transactionOperations;

    public ArticleUrlUniquenessGuard(
            ArticleRepository articleRepository,
            TransactionOperations transactionOperations
    ) {
        this.articleRepository = articleRepository;
        this.transactionOperations = transactionOperations;
    }

    public void validate(CurrentUser user, String url, UUID currentArticleId) {
        transactionOperations.executeWithoutResult(status -> articleRepository.findByUrlAndUserId(url, user.id())
                .filter(article -> currentArticleId == null || !article.getId().equals(currentArticleId))
                .ifPresent(article -> {
                    throw new DuplicateArticleUrlException(url, article.getId());
                }));
    }
}
