package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.observability.BackendMetrics;
import com.articleshelf.domain.article.Article;
import com.articleshelf.domain.article.ArticleNotFoundException;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleUrlUnavailableException;
import com.articleshelf.domain.article.ArticleVersionConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Objects;
import java.util.UUID;

@Service
public class UpdateArticleUseCase {
    private final ArticleRepository articleRepository;
    private final ArticleMetadataProvider metadataProvider;
    private final TransactionOperations transactionOperations;
    private final BackendMetrics metrics;
    private final ArticleTagResolver tagResolver;
    private final ArticleUrlUniquenessGuard urlUniquenessGuard;

    public UpdateArticleUseCase(
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

    public ArticleResponse updateArticle(CurrentUser user, UUID id, UpdateArticleCommand command) {
        Article current = loadArticleAndValidateUrl(user, id, command.url());
        if (command.version() != current.getVersion()) {
            throw new ArticleVersionConflictException(id);
        }
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
            current.replaceTags(tagResolver.resolve(user, command.tags()));

            ArticleResponse response = ArticleResponse.from(articleRepository.save(current));
            metrics.recordArticleUpdated();
            return response;
        }));
    }

    private Article loadArticleAndValidateUrl(CurrentUser user, UUID id, String url) {
        return Objects.requireNonNull(transactionOperations.execute(status -> {
            Article current = articleRepository.findByIdAndUserId(id, user.id()).orElseThrow(() -> new ArticleNotFoundException(id));
            urlUniquenessGuard.validate(user, url, id);
            return current;
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
