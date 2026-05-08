package com.articleshelf.config;

import com.articleshelf.application.auth.AuthService;
import com.articleshelf.application.auth.AuthUser;
import com.articleshelf.infrastructure.persistence.ArticleEntity;
import com.articleshelf.infrastructure.persistence.SpringDataArticleJpaRepository;
import com.articleshelf.infrastructure.persistence.SpringDataTagJpaRepository;
import com.articleshelf.infrastructure.persistence.TagEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class ArticleShelfDataInitializer implements ApplicationRunner {
    private final AuthService authService;
    private final SpringDataArticleJpaRepository articleRepository;
    private final SpringDataTagJpaRepository tagRepository;

    public ArticleShelfDataInitializer(
            AuthService authService,
            SpringDataArticleJpaRepository articleRepository,
            SpringDataTagJpaRepository tagRepository
    ) {
        this.authService = authService;
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<AuthUser> owner = authService.ensureInitialUser();
        owner.ifPresent(user -> assignOwnerToLegacyData(user.id()));
    }

    private void assignOwnerToLegacyData(UUID ownerId) {
        for (TagEntity tag : tagRepository.findAllByUserIdIsNull()) {
            tag.setUserId(ownerId);
            tagRepository.save(tag);
        }
        for (ArticleEntity article : articleRepository.findAllByUserIdIsNull()) {
            article.setUserId(ownerId);
            articleRepository.save(article);
        }
    }
}
