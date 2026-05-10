import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const RoutePlaceholder = { template: '<span />' }

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/articles' },
  { path: '/login', component: RoutePlaceholder },
  { path: '/register', component: RoutePlaceholder },
  { path: '/articles', component: RoutePlaceholder },
  { path: '/articles/:id', component: RoutePlaceholder },
  { path: '/calendar', component: RoutePlaceholder },
  { path: '/tags', component: RoutePlaceholder },
  { path: '/settings', component: RoutePlaceholder },
  { path: '/:pathMatch(.*)*', redirect: '/articles' }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})
