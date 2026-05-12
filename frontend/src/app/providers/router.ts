import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { pinia } from './pinia'
import { useAuthStore } from '../../features/auth/stores/auth'
import { ensureAuthReady } from '../../features/auth/services/ensureAuthReady'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/articles' },
  {
    path: '/login',
    component: () => import('../../features/auth/views/AuthRouteView.vue'),
    props: { mode: 'login' },
    meta: { guestOnly: true }
  },
  {
    path: '/register',
    component: () => import('../../features/auth/views/AuthRouteView.vue'),
    props: { mode: 'register' },
    meta: { guestOnly: true }
  },
  {
    path: '/articles',
    component: () => import('../../features/articles/views/WorkspaceRouteView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/articles/:id',
    component: () => import('../../features/articles/views/WorkspaceRouteView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/calendar',
    component: () => import('../../features/articles/views/WorkspaceRouteView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tags',
    component: () => import('../../features/articles/views/WorkspaceRouteView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    component: () => import('../../features/articles/views/WorkspaceRouteView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/extension/authorize',
    component: () => import('../../features/extension/views/ExtensionAuthorizeView.vue'),
    meta: { requiresAuth: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/articles' }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  await ensureAuthReady()

  const authStore = useAuthStore(pinia)
  const requiresAuth = to.meta.requiresAuth === true
  const guestOnly = to.meta.guestOnly === true

  if (requiresAuth && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        returnTo: to.fullPath
      }
    }
  }
  if (guestOnly && authStore.isAuthenticated) {
    const returnTo = typeof to.query.returnTo === 'string' && to.query.returnTo.startsWith('/')
      ? to.query.returnTo
      : '/articles'
    return returnTo
  }

  return true
})
