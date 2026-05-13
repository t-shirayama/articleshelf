package com.articleshelf;

import com.articleshelf.config.AuthProperties;
import com.articleshelf.config.AuthRateLimitProperties;
import com.articleshelf.config.ExtensionAuthProperties;
import com.articleshelf.config.OgpProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableConfigurationProperties({AuthProperties.class, AuthRateLimitProperties.class, ExtensionAuthProperties.class, OgpProperties.class})
public class ArticleShelfApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArticleShelfApplication.class, args);
    }
}
