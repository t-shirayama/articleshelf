package com.articleshelf.application.article;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.domain.article.Tag;
import com.articleshelf.domain.article.TagName;
import com.articleshelf.domain.article.TagRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArticleTagResolver {
    private final TagRepository tagRepository;

    public ArticleTagResolver(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Set<Tag> resolve(CurrentUser user, List<String> names) {
        if (names == null) {
            return new LinkedHashSet<>();
        }

        Set<Tag> tags = new LinkedHashSet<>();
        names.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .forEach(name -> tags.add(tagRepository.saveTag(user.id(), TagName.normalize(name))));
        return tags;
    }
}
