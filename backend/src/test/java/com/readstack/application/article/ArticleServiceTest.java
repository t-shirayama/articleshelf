package com.readstack.application.article;

import com.readstack.application.auth.CurrentUser;
import com.readstack.domain.article.Article;
import com.readstack.domain.article.ArticleNotFoundException;
import com.readstack.domain.article.ArticleRepository;
import com.readstack.domain.article.ArticleStatus;
import com.readstack.domain.article.ArticleUrlUnavailableException;
import com.readstack.domain.article.DuplicateArticleUrlException;
import com.readstack.domain.article.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleServiceTest {
    private final InMemoryArticleRepository repository = new InMemoryArticleRepository();
    private final StubMetadataProvider metadataProvider = new StubMetadataProvider();
    private final ArticleService service = new ArticleService(repository, metadataProvider);
    private final CurrentUser user = new CurrentUser(UUID.randomUUID(), "user@example.com", "User", List.of("USER"));

    @Test
    void addArticleUsesMetadataAndUserScopedTags() {
        metadataProvider.next = new ArticleMetadata("OGP title", "OGP summary", "https://example.com/image.png", true);

        ArticleResponse response = service.addArticle(user, new AddArticleCommand(
                "https://example.com/read",
                "",
                "",
                null,
                LocalDate.parse("2026-05-07"),
                true,
                4,
                "memo",
                List.of(" Java ", "", "java", "Spring")
        ));

        assertThat(response.title()).isEqualTo("OGP title");
        assertThat(response.summary()).isEqualTo("OGP summary");
        assertThat(response.favorite()).isTrue();
        assertThat(response.tags()).extracting(TagResponse::name).containsExactly("Java", "Spring");
        assertThat(repository.findAllByUserId(user.id())).singleElement()
                .satisfies(article -> assertThat(article.getUserId()).isEqualTo(user.id()));
    }

    @Test
    void addArticleRejectsDuplicateUrlWithinSameUserOnly() {
        service.addArticle(user, command("https://example.com/same", "First"));

        assertThatThrownBy(() -> service.addArticle(user, command("https://example.com/same", "Duplicate")))
                .isInstanceOf(DuplicateArticleUrlException.class);

        CurrentUser anotherUser = new CurrentUser(UUID.randomUUID(), "other@example.com", "Other", List.of("USER"));
        ArticleResponse response = service.addArticle(anotherUser, command("https://example.com/same", "Other"));

        assertThat(response.title()).isEqualTo("Other");
    }

    @Test
    void addArticleRejectsUnavailableMetadataWithoutSaving() {
        metadataProvider.next = ArticleMetadata.unavailable();

        assertThatThrownBy(() -> service.addArticle(user, command("https://example.com/down", "Down")))
                .isInstanceOf(ArticleUrlUnavailableException.class);

        assertThat(repository.findAllByUserId(user.id())).isEmpty();
    }

    @Test
    void findArticlesAppliesStatusTagSearchAndFavoriteFilters() {
        service.addArticle(user, new AddArticleCommand(
                "https://example.com/vue",
                "Vue memo",
                "frontend",
                ArticleStatus.READ,
                LocalDate.parse("2026-05-07"),
                true,
                5,
                "pinia note",
                List.of("Vue")
        ));
        service.addArticle(user, new AddArticleCommand(
                "https://example.com/java",
                "Java memo",
                "backend",
                ArticleStatus.UNREAD,
                null,
                false,
                3,
                "spring note",
                List.of("Java")
        ));

        List<ArticleResponse> result = service.findArticles(user, ArticleStatus.READ, "vue", "pinia", true);

        assertThat(result).singleElement()
                .satisfies(article -> assertThat(article.title()).isEqualTo("Vue memo"));
    }

    @Test
    void deleteArticleIsUserScoped() {
        ArticleResponse response = service.addArticle(user, command("https://example.com/delete", "Delete"));
        CurrentUser anotherUser = new CurrentUser(UUID.randomUUID(), "other@example.com", "Other", List.of("USER"));

        assertThatThrownBy(() -> service.deleteArticle(anotherUser, response.id()))
                .isInstanceOf(ArticleNotFoundException.class);

        service.deleteArticle(user, response.id());

        assertThat(repository.findAllByUserId(user.id())).isEmpty();
    }

    @Test
    void updateArticleDoesNotRefetchMetadataWhenUrlDoesNotChange() {
        metadataProvider.next = new ArticleMetadata("Original", "Summary", "https://example.com/thumb.png", true);
        ArticleResponse response = service.addArticle(user, command("https://example.com/original", "Original"));
        metadataProvider.fetchCount = 0;

        ArticleResponse updated = service.updateArticle(user, response.id(), new UpdateArticleCommand(
                "https://example.com/original",
                "Updated title",
                "Updated summary",
                ArticleStatus.UNREAD,
                null,
                true,
                2,
                "Updated notes",
                List.of("Java")
        ));

        assertThat(metadataProvider.fetchCount).isZero();
        assertThat(updated.title()).isEqualTo("Updated title");
        assertThat(updated.favorite()).isTrue();
    }

    @Test
    void updateArticleRejectsDuplicateUrlWithinSameUserOnly() {
        ArticleResponse first = service.addArticle(user, command("https://example.com/first", "First"));
        service.addArticle(user, command("https://example.com/second", "Second"));

        assertThatThrownBy(() -> service.updateArticle(user, first.id(), new UpdateArticleCommand(
                "https://example.com/second",
                "First",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                0,
                "",
                List.of()
        ))).isInstanceOf(DuplicateArticleUrlException.class);

        CurrentUser anotherUser = new CurrentUser(UUID.randomUUID(), "other@example.com", "Other", List.of("USER"));
        service.addArticle(anotherUser, command("https://example.com/shared", "Other"));
        ArticleResponse ownArticle = service.addArticle(user, command("https://example.com/user-owned", "Mine"));

        ArticleResponse updated = service.updateArticle(user, ownArticle.id(), new UpdateArticleCommand(
                "https://example.com/shared",
                "Mine updated",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                0,
                "",
                List.of()
        ));

        assertThat(updated.url()).isEqualTo("https://example.com/shared");
    }

    @Test
    void updateArticleClearsReadDateWhenMovedBackToUnread() {
        ArticleResponse response = service.addArticle(user, new AddArticleCommand(
                "https://example.com/read",
                "Read article",
                "",
                ArticleStatus.READ,
                LocalDate.parse("2026-05-07"),
                false,
                1,
                "",
                List.of()
        ));

        ArticleResponse updated = service.updateArticle(user, response.id(), new UpdateArticleCommand(
                response.url(),
                response.title(),
                response.summary(),
                ArticleStatus.UNREAD,
                null,
                false,
                1,
                "",
                List.of()
        ));

        assertThat(updated.status()).isEqualTo(ArticleStatus.UNREAD);
        assertThat(updated.readDate()).isNull();
    }

    @Test
    void updateArticleClampsRatingAndReplacesTags() {
        ArticleResponse response = service.addArticle(user, new AddArticleCommand(
                "https://example.com/tags",
                "Tag article",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                3,
                "",
                List.of("Java", "Spring")
        ));

        ArticleResponse updated = service.updateArticle(user, response.id(), new UpdateArticleCommand(
                response.url(),
                response.title(),
                response.summary(),
                ArticleStatus.UNREAD,
                null,
                false,
                -1,
                "",
                List.of("Vue")
        ));

        assertThat(updated.rating()).isZero();
        assertThat(updated.tags()).extracting(TagResponse::name).containsExactly("Vue");
        assertThat(updated.tags()).extracting(TagResponse::name).doesNotContain("Java", "Spring");
    }

    private AddArticleCommand command(String url, String title) {
        return new AddArticleCommand(url, title, "", ArticleStatus.UNREAD, null, false, 0, "", List.of());
    }

    private static class StubMetadataProvider implements ArticleMetadataProvider {
        private ArticleMetadata next = new ArticleMetadata("", "", "", true);
        private int fetchCount = 0;

        @Override
        public ArticleMetadata fetch(String url) {
            fetchCount += 1;
            return next;
        }
    }

    private static class InMemoryArticleRepository implements ArticleRepository {
        private final Map<UUID, Article> articles = new LinkedHashMap<>();
        private final Map<UUID, Tag> tags = new LinkedHashMap<>();

        @Override
        public List<Article> findAllByUserId(UUID userId) {
            return articles.values().stream()
                    .filter(article -> article.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<Article> findByIdAndUserId(UUID id, UUID userId) {
            return Optional.ofNullable(articles.get(id))
                    .filter(article -> article.getUserId().equals(userId));
        }

        @Override
        public Optional<Article> findByUrlAndUserId(String url, UUID userId) {
            return articles.values().stream()
                    .filter(article -> article.getUserId().equals(userId))
                    .filter(article -> article.getUrl().equals(url))
                    .findFirst();
        }

        @Override
        public boolean existsByUrlAndUserId(String url, UUID userId) {
            return findByUrlAndUserId(url, userId).isPresent();
        }

        @Override
        public boolean existsByUrlAndUserIdAndIdNot(String url, UUID userId, UUID id) {
            return findByUrlAndUserId(url, userId)
                    .filter(article -> !article.getId().equals(id))
                    .isPresent();
        }

        @Override
        public Article save(Article article) {
            Article persisted = new Article(
                    article.getId(),
                    article.getUserId(),
                    article.getUrl(),
                    article.getTitle(),
                    article.getSummary(),
                    article.getThumbnailUrl(),
                    article.getStatus(),
                    article.getReadDate(),
                    article.isFavorite(),
                    article.getRating(),
                    article.getNotes(),
                    article.getTags(),
                    article.getCreatedAt() == null ? Instant.now() : article.getCreatedAt(),
                    Instant.now()
            );
            articles.put(persisted.getId(), persisted);
            return persisted;
        }

        @Override
        public void deleteByIdAndUserId(UUID id, UUID userId) {
            findByIdAndUserId(id, userId).ifPresent(article -> articles.remove(article.getId()));
        }

        @Override
        public List<Tag> findAllTagsByUserId(UUID userId) {
            return tags.values().stream()
                    .filter(tag -> tag.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Tag saveTag(UUID userId, String name) {
            String normalized = name == null ? "" : name.trim();
            return tags.values().stream()
                    .filter(tag -> tag.getUserId().equals(userId))
                    .filter(tag -> tag.getName().equalsIgnoreCase(normalized))
                    .findFirst()
                    .orElseGet(() -> {
                        Tag tag = new Tag(UUID.randomUUID(), userId, normalized, Instant.now(), Instant.now());
                        tags.put(tag.getId(), tag);
                        return tag;
                    });
        }
    }
}
