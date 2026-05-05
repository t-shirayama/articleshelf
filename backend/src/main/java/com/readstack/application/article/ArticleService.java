package com.readstack.application.article;

import com.readstack.domain.article.Article;
import com.readstack.domain.article.ArticleNotFoundException;
import com.readstack.domain.article.ArticleRepository;
import com.readstack.domain.article.ArticleStatus;
import com.readstack.domain.article.DuplicateArticleUrlException;
import com.readstack.domain.article.Tag;
import com.readstack.infrastructure.ogp.OgpMetadata;
import com.readstack.infrastructure.ogp.OgpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final OgpService ogpService;

    public ArticleService(ArticleRepository articleRepository, OgpService ogpService) {
        this.articleRepository = articleRepository;
        this.ogpService = ogpService;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findArticles(ArticleStatus status, String tag, String search, Boolean favorite) {
        String normalizedTag = normalizeFilter(tag);
        String normalizedSearch = normalizeFilter(search);

        return articleRepository.findAll().stream()
                .filter(article -> status == null || article.getStatus() == status)
                .filter(article -> favorite == null || article.isFavorite() == favorite)
                .filter(article -> normalizedTag == null || article.getTags().stream()
                        .anyMatch(item -> item.getName().equalsIgnoreCase(normalizedTag)))
                .filter(article -> normalizedSearch == null || matchesSearch(article, normalizedSearch))
                .sorted(Comparator.comparing(Article::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ArticleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse findArticle(UUID id) {
        return articleRepository.findById(id)
                .map(ArticleResponse::from)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }

    @Transactional
    public ArticleResponse addArticle(AddArticleCommand command) {
        if (articleRepository.existsByUrl(command.url())) {
            throw new DuplicateArticleUrlException(command.url());
        }

        OgpMetadata metadata = ogpService.fetch(command.url());
        Article article = new Article(
                null,
                command.url(),
                firstPresent(command.title(), metadata.title(), command.url()),
                firstPresent(command.summary(), metadata.description(), ""),
                command.status(),
                command.readDate(),
                Boolean.TRUE.equals(command.favorite()),
                command.notes(),
                resolveTags(command.tags()),
                null,
                null
        );

        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse updateArticle(UUID id, UpdateArticleCommand command) {
        Article current = articleRepository.findById(id).orElseThrow(() -> new ArticleNotFoundException(id));
        if (articleRepository.existsByUrlAndIdNot(command.url(), id)) {
            throw new DuplicateArticleUrlException(command.url());
        }

        Article updated = new Article(
                current.getId(),
                command.url(),
                firstPresent(command.title(), current.getTitle()),
                command.summary(),
                command.status() == null ? current.getStatus() : command.status(),
                command.readDate(),
                command.favorite() == null ? current.isFavorite() : command.favorite(),
                command.notes(),
                resolveTags(command.tags()),
                current.getCreatedAt(),
                current.getUpdatedAt()
        );

        return ArticleResponse.from(articleRepository.save(updated));
    }

    @Transactional
    public void deleteArticle(UUID id) {
        if (articleRepository.findById(id).isEmpty()) {
            throw new ArticleNotFoundException(id);
        }
        articleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> findTags() {
        return articleRepository.findAllTags().stream()
                .map(TagResponse::from)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList();
    }

    @Transactional
    public TagResponse addTag(String name) {
        return TagResponse.from(articleRepository.saveTag(name));
    }

    private Set<Tag> resolveTags(List<String> names) {
        if (names == null) {
            return new LinkedHashSet<>();
        }

        Set<Tag> tags = new LinkedHashSet<>();
        names.stream()
                .map(this::normalizeName)
                .filter(value -> !value.isBlank())
                .distinct()
                .forEach(name -> tags.add(articleRepository.saveTag(name)));
        return tags;
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

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
