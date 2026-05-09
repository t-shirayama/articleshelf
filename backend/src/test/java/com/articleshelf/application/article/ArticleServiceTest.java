package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.Article;
import com.articleshelf.domain.article.ArticleNotFoundException;
import com.articleshelf.domain.article.ArticleRepository;
import com.articleshelf.domain.article.ArticleSearchCriteria;
import com.articleshelf.domain.article.ArticleStatus;
import com.articleshelf.domain.article.ArticleUrlUnavailableException;
import com.articleshelf.domain.article.DuplicateArticleUrlException;
import com.articleshelf.domain.article.Tag;
import com.articleshelf.domain.article.TagNotFoundException;
import com.articleshelf.domain.article.TagRepository;
import com.articleshelf.domain.article.TagUsage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleServiceTest {
    private final InMemoryArticleRepository repository = new InMemoryArticleRepository();
    private final StubMetadataProvider metadataProvider = new StubMetadataProvider();
    private final ArticleService service = new ArticleService(repository, repository, metadataProvider);
    private final CurrentUser user = new CurrentUser(UUID.randomUUID(), "user", "User", List.of("USER"));

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

        CurrentUser anotherUser = new CurrentUser(UUID.randomUUID(), "other", "Other", List.of("USER"));
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
        CurrentUser anotherUser = new CurrentUser(UUID.randomUUID(), "other", "Other", List.of("USER"));

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

        CurrentUser anotherUser = new CurrentUser(UUID.randomUUID(), "other", "Other", List.of("USER"));
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

    private static class InMemoryArticleRepository implements ArticleRepository, TagRepository {
        private final Map<UUID, Article> articles = new LinkedHashMap<>();
        private final Map<UUID, Tag> tags = new LinkedHashMap<>();

        @Override
        public List<Article> findAllByUserId(UUID userId) {
            return articles.values().stream()
                    .filter(article -> article.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public List<Article> searchByUserId(UUID userId, ArticleSearchCriteria criteria) {
            return findAllByUserId(userId).stream()
                    .filter(article -> criteria.status() == null || article.getStatus() == criteria.status())
                    .filter(article -> criteria.favorite() == null || article.isFavorite() == criteria.favorite())
                    .filter(article -> criteria.tag() == null || article.getTags().stream()
                            .anyMatch(tag -> tag.getName().equalsIgnoreCase(criteria.tag())))
                    .filter(article -> criteria.search() == null || matchesSearch(article, criteria.search()))
                    .sorted(Comparator.comparing(Article::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
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

        public List<Tag> findAllTagsByUserId(UUID userId) {
            return tags.values().stream()
                    .filter(tag -> tag.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public List<TagUsage> findAllTagUsagesByUserId(UUID userId) {
            return findAllTagsByUserId(userId).stream()
                    .map(tag -> new TagUsage(tag, countArticlesByTagIdAndUserId(tag.getId(), userId)))
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

        @Override
        public Optional<Tag> findTagByIdAndUserId(UUID id, UUID userId) {
            return Optional.ofNullable(tags.get(id))
                    .filter(tag -> tag.getUserId().equals(userId));
        }

        @Override
        public Optional<Tag> findTagByNameAndUserId(String name, UUID userId) {
            String normalized = name == null ? "" : name.trim();
            return tags.values().stream()
                    .filter(tag -> tag.getUserId().equals(userId))
                    .filter(tag -> tag.getName().equalsIgnoreCase(normalized))
                    .findFirst();
        }

        @Override
        public long countArticlesByTagIdAndUserId(UUID tagId, UUID userId) {
            return articles.values().stream()
                    .filter(article -> article.getUserId().equals(userId))
                    .filter(article -> article.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
                    .count();
        }

        @Override
        public Tag renameTag(UUID userId, UUID tagId, String name) {
            Tag current = findTagByIdAndUserId(tagId, userId).orElseThrow(() -> new TagNotFoundException(tagId));
            Tag renamed = new Tag(current.getId(), current.getUserId(), name, current.getCreatedAt(), Instant.now());
            tags.put(renamed.getId(), renamed);
            articles.replaceAll((id, article) -> {
                Set<Tag> updatedTags = article.getTags().stream()
                        .map(tag -> tag.getId().equals(tagId) ? renamed : tag)
                        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
                return copyArticle(article, updatedTags);
            });
            return renamed;
        }

        @Override
        public void mergeTags(UUID userId, UUID sourceTagId, UUID targetTagId) {
            Tag target = findTagByIdAndUserId(targetTagId, userId).orElseThrow(() -> new TagNotFoundException(targetTagId));
            findTagByIdAndUserId(sourceTagId, userId).orElseThrow(() -> new TagNotFoundException(sourceTagId));
            articles.replaceAll((id, article) -> {
                if (!article.getUserId().equals(userId)) {
                    return article;
                }
                Set<Tag> updatedTags = article.getTags().stream()
                        .map(tag -> tag.getId().equals(sourceTagId) ? target : tag)
                        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
                return copyArticle(article, updatedTags);
            });
            tags.remove(sourceTagId);
        }

        @Override
        public void deleteTagByIdAndUserId(UUID tagId, UUID userId) {
            findTagByIdAndUserId(tagId, userId).ifPresent(tag -> tags.remove(tag.getId()));
        }

        private boolean matchesSearch(Article article, String search) {
            String haystack = String.join(" ",
                    nullToEmpty(article.getTitle()),
                    nullToEmpty(article.getUrl()),
                    nullToEmpty(article.getSummary()),
                    nullToEmpty(article.getNotes())
            ).toLowerCase(Locale.ROOT);
            return haystack.contains(search);
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }

        private Article copyArticle(Article article, Set<Tag> tags) {
            return new Article(
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
                    tags,
                    article.getCreatedAt(),
                    Instant.now()
            );
        }
    }
}
