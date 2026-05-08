package com.readstack.config;

import com.readstack.application.auth.AuthService;
import com.readstack.infrastructure.persistence.SpringDataArticleJpaRepository;
import com.readstack.infrastructure.persistence.SpringDataTagJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArticleShelfDataInitializerTest {
    @Test
    void doesNotAssignLegacyDataWhenInitialUserIsDisabled() {
        AuthService authService = mock(AuthService.class);
        SpringDataArticleJpaRepository articleRepository = mock(SpringDataArticleJpaRepository.class);
        SpringDataTagJpaRepository tagRepository = mock(SpringDataTagJpaRepository.class);
        ArticleShelfDataInitializer initializer = new ArticleShelfDataInitializer(
                authService,
                articleRepository,
                tagRepository
        );

        when(authService.ensureInitialUser()).thenReturn(Optional.empty());
        initializer.run(mock(ApplicationArguments.class));

        verifyNoInteractions(articleRepository, tagRepository);
    }
}
