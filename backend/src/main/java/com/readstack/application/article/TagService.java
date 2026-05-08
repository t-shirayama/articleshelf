package com.readstack.application.article;

import com.readstack.application.auth.CurrentUser;
import com.readstack.domain.article.DuplicateTagNameException;
import com.readstack.domain.article.Tag;
import com.readstack.domain.article.TagInUseException;
import com.readstack.domain.article.TagNotFoundException;
import com.readstack.domain.article.TagRepository;
import com.readstack.domain.article.TagUsage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> findTags(CurrentUser user) {
        return tagRepository.findAllTagUsagesByUserId(user.id()).stream()
                .map(TagResponse::from)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList();
    }

    @Transactional
    public TagResponse addTag(CurrentUser user, String name) {
        return TagResponse.from(tagRepository.saveTag(user.id(), name));
    }

    @Transactional
    public TagResponse renameTag(CurrentUser user, UUID id, String name) {
        Tag current = tagRepository.findTagByIdAndUserId(id, user.id())
                .orElseThrow(() -> new TagNotFoundException(id));
        String normalized = normalizeName(name);
        tagRepository.findTagByNameAndUserId(normalized, user.id())
                .filter(tag -> !tag.getId().equals(current.getId()))
                .ifPresent(tag -> {
                    throw new DuplicateTagNameException(normalized);
                });
        Tag renamed = tagRepository.renameTag(user.id(), id, normalized);
        long articleCount = tagRepository.countArticlesByTagIdAndUserId(id, user.id());
        return TagResponse.from(new TagUsage(renamed, articleCount));
    }

    @Transactional
    public void mergeTags(CurrentUser user, UUID sourceId, UUID targetId) {
        if (sourceId.equals(targetId)) {
            throw DuplicateTagNameException.mergeTargetSame();
        }
        tagRepository.findTagByIdAndUserId(sourceId, user.id())
                .orElseThrow(() -> new TagNotFoundException(sourceId));
        tagRepository.findTagByIdAndUserId(targetId, user.id())
                .orElseThrow(() -> new TagNotFoundException(targetId));
        tagRepository.mergeTags(user.id(), sourceId, targetId);
    }

    @Transactional
    public void deleteUnusedTag(CurrentUser user, UUID id) {
        Tag tag = tagRepository.findTagByIdAndUserId(id, user.id())
                .orElseThrow(() -> new TagNotFoundException(id));
        long articleCount = tagRepository.countArticlesByTagIdAndUserId(id, user.id());
        if (articleCount > 0) {
            throw new TagInUseException(tag.getName(), articleCount);
        }
        tagRepository.deleteTagByIdAndUserId(id, user.id());
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }
}
