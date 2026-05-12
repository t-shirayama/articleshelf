package com.articleshelf.infrastructure.persistence;

import com.articleshelf.application.article.ArticleListQuery;
import com.articleshelf.domain.article.Article;
import com.articleshelf.domain.article.ArticleSearchCriteria;
import com.articleshelf.domain.article.ArticleStatus;
import com.articleshelf.domain.article.ArticleVersionConflictException;
import com.articleshelf.domain.article.Tag;
import com.articleshelf.domain.article.TagUsage;
import com.articleshelf.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://db:5432/articleshelf",
        "spring.datasource.username=articleshelf",
        "spring.datasource.password=articleshelf",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "articleshelf.frontend-origin=http://localhost:5173",
        "articleshelf.auth.access-token-secret=test-articleshelf-access-secret-change-me-please-32bytes",
        "articleshelf.auth.refresh-token-hash-secret=test-articleshelf-refresh-hash-secret-change-me",
        "articleshelf.auth.cookie-secure=false",
        "articleshelf.auth.cookie-same-site=Lax",
        "articleshelf.auth.csrf-enabled=false",
        "articleshelf.auth.initial-user-enabled=false",
        "articleshelf.auth.initial-username=owner-test",
        "articleshelf.auth.initial-user-password=password123"
})
class JpaArticleRepositoryPostgresIntegrationTest {
    @Autowired
    private JpaArticleRepository repository;

    @Autowired
    private JpaTagRepository tagRepository;

    @Autowired
    private SpringDataArticleJpaRepository articleJpaRepository;

    @Autowired
    private SpringDataUserJpaRepository userJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE article_tags, articles, tags, refresh_tokens, users CASCADE");
    }

    @Test
    void postgresSchemaAllowsSameUrlAcrossUsersButRejectsDuplicatesPerUser() {
        UUID userA = createUser("postgres-a");
        UUID userB = createUser("postgres-b");

        repository.save(article(userA, "https://example.com/shared", 4, LocalDate.parse("2026-05-07"), Set.of(tag(userA, "Vue"))));
        repository.save(article(userB, "https://example.com/shared", 2, null, Set.of(tag(userB, "Java"))));

        assertThat(repository.findAllByUserId(userA)).singleElement()
                .satisfies(article -> {
                    assertThat(article.getRating()).isEqualTo(4);
                    assertThat(article.getReadDate()).isEqualTo(LocalDate.parse("2026-05-07"));
                    assertThat(article.getTags()).extracting(Tag::getName).containsExactly("Vue");
                });
        assertThat(repository.findAllByUserId(userB)).singleElement()
                .satisfies(article -> assertThat(article.getTags()).extracting(Tag::getName).containsExactly("Java"));

        assertThatThrownBy(() -> {
            repository.save(article(userA, "https://example.com/shared", 1, null, Set.of()));
            articleJpaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void postgresPersistenceReplacesArticleTagsWithoutLeavingStaleRelations() {
        UUID userId = createUser("postgres-tags");
        Article saved = repository.save(article(
                userId,
                "https://example.com/tags",
                3,
                null,
                new LinkedHashSet<>(List.of(tag(userId, "Java"), tag(userId, "Spring")))
        ));

        Article updated = repository.save(new Article(
                saved.getId(),
                userId,
                saved.getVersion(),
                saved.getUrl(),
                saved.getTitle(),
                saved.getSummary(),
                saved.getThumbnailUrl(),
                ArticleStatus.READ,
                LocalDate.parse("2026-05-08"),
                true,
                5,
                "updated note",
                new LinkedHashSet<>(List.of(tag(userId, "Vue"))),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        ));
        articleJpaRepository.flush();

        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.READ);
        assertThat(updated.getReadDate()).isEqualTo(LocalDate.parse("2026-05-08"));
        assertThat(updated.getTags()).extracting(Tag::getName).containsExactly("Vue");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM article_tags WHERE article_id = ?",
                Integer.class,
                updated.getId()
        )).isEqualTo(1);
    }

    @Test
    void postgresSaveRejectsStaleArticleVersion() {
        UUID userId = createUser("postgres-version");
        Article saved = repository.save(article(userId, "https://example.com/versioned", 3, null, Set.of(tag(userId, "Vue"))));

        Article stale = new Article(
                saved.getId(),
                userId,
                saved.getVersion(),
                saved.getUrl(),
                "Updated once",
                saved.getSummary(),
                saved.getThumbnailUrl(),
                saved.getStatus(),
                saved.getReadDate(),
                saved.isFavorite(),
                saved.getRating(),
                saved.getNotes(),
                saved.getTags(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
        Article firstUpdate = repository.save(stale);

        Article staleRetry = new Article(
                firstUpdate.getId(),
                userId,
                saved.getVersion(),
                firstUpdate.getUrl(),
                "Stale retry",
                firstUpdate.getSummary(),
                firstUpdate.getThumbnailUrl(),
                firstUpdate.getStatus(),
                firstUpdate.getReadDate(),
                firstUpdate.isFavorite(),
                firstUpdate.getRating(),
                firstUpdate.getNotes(),
                firstUpdate.getTags(),
                firstUpdate.getCreatedAt(),
                firstUpdate.getUpdatedAt()
        );

        assertThatThrownBy(() -> repository.save(staleRetry))
                .isInstanceOf(ArticleVersionConflictException.class);
    }

    @Test
    void postgresSearchAppliesOptionalFiltersWithoutTypeInferenceErrors() {
        UUID userId = createUser("postgres-search");
        Tag vue = tag(userId, "Vue");
        Tag java = tag(userId, "Java");

        repository.save(new Article(
                null,
                userId,
                0L,
                "https://example.com/pinia",
                "Pinia patterns",
                "Vue store summary",
                "",
                ArticleStatus.READ,
                LocalDate.parse("2026-05-07"),
                true,
                4,
                "State management note",
                Set.of(vue),
                Instant.now(),
                Instant.now()
        ));
        repository.save(new Article(
                null,
                userId,
                0L,
                "https://example.com/spring",
                "Spring guide",
                "Backend summary",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                3,
                "Java note",
                Set.of(java),
                Instant.now(),
                Instant.now()
        ));

        List<Article> result = repository.searchByUserId(
                userId,
                new ArticleSearchCriteria(ArticleStatus.READ, "vue", "pinia", true),
                new ArticleListQuery(null, null, null)
        );

        assertThat(result).singleElement()
                .satisfies(article -> assertThat(article.getUrl()).isEqualTo("https://example.com/pinia"));
    }

    @Test
    void postgresSearchTreatsPercentAndUnderscoreAsLiteralText() {
        UUID userId = createUser("postgres-search-wildcards");

        repository.save(new Article(
                null,
                userId,
                0L,
                "https://example.com/percent",
                "100% coverage",
                "",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                0,
                "",
                Set.of(),
                Instant.now(),
                Instant.now()
        ));
        repository.save(new Article(
                null,
                userId,
                0L,
                "https://example.com/wide-percent",
                "100x coverage",
                "",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                0,
                "",
                Set.of(),
                Instant.now(),
                Instant.now()
        ));
        repository.save(new Article(
                null,
                userId,
                0L,
                "https://example.com/underscore",
                "foo_bar note",
                "",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                0,
                "",
                Set.of(),
                Instant.now(),
                Instant.now()
        ));
        repository.save(new Article(
                null,
                userId,
                0L,
                "https://example.com/wide-underscore",
                "fooXbar note",
                "",
                "",
                ArticleStatus.UNREAD,
                null,
                false,
                0,
                "",
                Set.of(),
                Instant.now(),
                Instant.now()
        ));

        List<Article> percentResult = repository.searchByUserId(
                userId,
                new ArticleSearchCriteria(null, null, "100%", null),
                new ArticleListQuery(null, null, null)
        );
        List<Article> underscoreResult = repository.searchByUserId(
                userId,
                new ArticleSearchCriteria(null, null, "foo_bar", null),
                new ArticleListQuery(null, null, null)
        );

        assertThat(percentResult).extracting(Article::getUrl)
                .containsExactly("https://example.com/percent");
        assertThat(underscoreResult).extracting(Article::getUrl)
                .containsExactly("https://example.com/underscore");
    }

    @Test
    void postgresSearchAppliesDbBackedPaginationAndSort() {
        UUID userId = createUser("postgres-page-sort");
        Tag tag = tag(userId, "Architecture");
        repository.save(article(userId, "https://example.com/a", "Gamma", 2, LocalDate.parse("2026-05-02"), Set.of(tag), Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-03T00:00:00Z")));
        repository.save(article(userId, "https://example.com/b", "Alpha", 5, LocalDate.parse("2026-05-05"), Set.of(tag), Instant.parse("2026-05-02T00:00:00Z"), Instant.parse("2026-05-05T00:00:00Z")));
        repository.save(article(userId, "https://example.com/c", "Beta", 3, null, Set.of(tag), Instant.parse("2026-05-03T00:00:00Z"), Instant.parse("2026-05-04T00:00:00Z")));

        List<Article> titlePage = repository.searchByUserId(
                userId,
                new ArticleSearchCriteria(null, "architecture", null, null),
                new ArticleListQuery(0, 2, "TITLE_ASC")
        );
        List<Article> ratingPage = repository.searchByUserId(
                userId,
                new ArticleSearchCriteria(null, null, null, null),
                new ArticleListQuery(0, 2, "RATING_DESC")
        );
        List<Article> readDateOrder = repository.searchByUserId(
                userId,
                new ArticleSearchCriteria(null, null, null, null),
                new ArticleListQuery(null, null, "READ_DATE_DESC")
        );

        assertThat(titlePage).extracting(Article::getTitle)
                .containsExactly("Alpha", "Beta");
        assertThat(ratingPage).extracting(Article::getRating)
                .containsExactly(5, 3);
        assertThat(readDateOrder).extracting(Article::getTitle)
                .containsExactly("Alpha", "Gamma", "Beta");
    }

    @Test
    void postgresTagUsagesReturnCountsForAllTagsInOneRepositoryCall() {
        UUID userId = createUser("postgres-tag-usages");
        Tag used = tag(userId, "Used");
        tag(userId, "Unused");
        repository.save(article(userId, "https://example.com/used-tag", 3, null, Set.of(used)));

        List<TagUsage> usages = tagRepository.findAllTagUsagesByUserId(userId);

        assertThat(usages).hasSize(2);
        assertThat(usages).anySatisfy(usage -> {
            assertThat(usage.tag().getName()).isEqualTo("Used");
            assertThat(usage.articleCount()).isEqualTo(1);
        });
        assertThat(usages).anySatisfy(usage -> {
            assertThat(usage.tag().getName()).isEqualTo("Unused");
            assertThat(usage.articleCount()).isZero();
        });
    }

    private UUID createUser(String username) {
        UserEntity user = new UserEntity();
        user.setUsername(username.substring(0, Math.min(username.length(), 32)));
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Test User");
        user.setRole("USER");
        user.setStatus(UserStatus.ACTIVE);
        return userJpaRepository.save(user).getId();
    }

    private Article article(UUID userId, String url, int rating, LocalDate readDate, Set<Tag> tags) {
        Instant now = Instant.now();
        return article(userId, url, "Stored article", rating, readDate, tags, now, now);
    }

    private Article article(UUID userId, String url, String title, int rating, LocalDate readDate, Set<Tag> tags, Instant createdAt, Instant updatedAt) {
        return new Article(
                null,
                userId,
                0L,
                url,
                title,
                "Stored summary",
                "",
                readDate == null ? ArticleStatus.UNREAD : ArticleStatus.READ,
                readDate,
                false,
                rating,
                "Stored note",
                tags,
                createdAt,
                updatedAt
        );
    }

    private Tag tag(UUID userId, String name) {
        return tagRepository.saveTag(userId, name);
    }
}
