package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.observability.BackendMetrics;
import com.articleshelf.domain.article.Article;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleUrlUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Objects;

@Service
public class AddArticleUseCase {
    private final ArticleRepository articleRepository;
    private final ArticleMetadataProvider metadataProvider;
    private final TransactionOperations transactionOperations;
    private final BackendMetrics metrics;
    private final ArticleTagResolver tagResolver;
    private final ArticleUrlUniquenessGuard urlUniquenessGuard;

    public AddArticleUseCase(
            ArticleRepository articleRepository,
            ArticleMetadataProvider metadataProvider,
            TransactionOperations transactionOperations,
            BackendMetrics metrics,
            ArticleTagResolver tagResolver,
            ArticleUrlUniquenessGuard urlUniquenessGuard
    ) {
        this.articleRepository = articleRepository;
        this.metadataProvider = metadataProvider;
        this.transactionOperations = transactionOperations;
        this.metrics = metrics;
        this.tagResolver = tagResolver;
        this.urlUniquenessGuard = urlUniquenessGuard;
    }

    public ArticleResponse addArticle(CurrentUser user, AddArticleCommand command) {
        urlUniquenessGuard.validate(user, command.url(), null);

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
                    tagResolver.resolve(user, command.tags()),
                    null,
                    null
            );

            ArticleResponse response = ArticleResponse.from(articleRepository.save(article));
            metrics.recordArticleCreated();
            return response;
        }));
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
