package com.articleshelf.adapter.web;

import com.articleshelf.application.article.AddArticleCommand;
import com.articleshelf.application.article.UpdateArticleCommand;
import org.springframework.stereotype.Component;

@Component
public class ArticleRequestMapper {
    public AddArticleCommand toAddCommand(ArticleRequest request) {
        return new AddArticleCommand(
                request.url(),
                request.title(),
                request.summary(),
                request.status(),
                request.readDate(),
                request.favorite(),
                request.rating(),
                request.notes(),
                request.tags()
        );
    }

    public UpdateArticleCommand toUpdateCommand(ArticleRequest request) {
        return new UpdateArticleCommand(
                request.url(),
                request.title(),
                request.summary(),
                request.status(),
                request.readDate(),
                request.favorite(),
                request.rating(),
                request.notes(),
                request.tags()
        );
    }
}
