import type { Article, ArticleInput } from '../types'

export function replaceArticle(articles: Article[], updated: Article): Article[] {
  return articles.map((article) => article.id === updated.id ? updated : article)
}

export function toArticleInput(article: Article): ArticleInput {
  return {
    id: article.id,
    version: article.version,
    url: article.url,
    title: article.title,
    summary: article.summary || '',
    status: article.status,
    readDate: article.readDate || null,
    favorite: article.favorite,
    rating: article.rating,
    notes: article.notes || '',
    tags: article.tags?.map((tag) => tag.name).filter(Boolean) || []
  }
}
