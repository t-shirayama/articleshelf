const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })

  if (response.status === 204) {
    return null
  }

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const message = payload?.messages?.join(', ') || 'API request failed'
    throw new Error(message)
  }
  return payload
}

export const api = {
  findArticles(filters = {}) {
    const params = new URLSearchParams()
    if (filters.status && filters.status !== 'ALL') params.set('status', filters.status)
    if (filters.tag) params.set('tag', filters.tag)
    if (filters.search) params.set('search', filters.search)
    if (filters.favorite) params.set('favorite', 'true')
    const query = params.toString()
    return request(`/api/articles${query ? `?${query}` : ''}`)
  },
  findArticle(id) {
    return request(`/api/articles/${id}`)
  },
  createArticle(article) {
    return request('/api/articles', {
      method: 'POST',
      body: JSON.stringify(article)
    })
  },
  updateArticle(id, article) {
    return request(`/api/articles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(article)
    })
  },
  deleteArticle(id) {
    return request(`/api/articles/${id}`, {
      method: 'DELETE'
    })
  },
  findTags() {
    return request('/api/tags')
  },
  createTag(name) {
    return request('/api/tags', {
      method: 'POST',
      body: JSON.stringify({ name })
    })
  }
}
