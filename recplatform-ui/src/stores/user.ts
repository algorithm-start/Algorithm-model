import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type UserInfo } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  /** Menu keys this user is allowed to see. */
  const menus = computed<string[]>(() => userInfo.value?.menus ?? [])

  /** Whether the user has ADMIN role. */
  const isAdmin = computed(() =>
    userInfo.value?.roles?.some((r: any) => r.code === 'ADMIN') ?? false
  )

  /** Whether the user is a read-only VIEWER (and not also ADMIN/USER). */
  const isViewer = computed(() => {
    const roles = userInfo.value?.roles ?? []
    if (!roles.length) return false
    const hasViewer = roles.some((r: any) => r.code === 'VIEWER')
    const hasWriteRole = roles.some((r: any) => r.code === 'ADMIN' || r.code === 'USER')
    return hasViewer && !hasWriteRole
  })

  /** Whether the user is allowed to edit/save algorithm configuration. */
  const canEditAlgorithm = computed(() => !isViewer.value)

  /**
   * Check if a top-level menu key (e.g. "solver", "admin") is visible.
   * Also accepts sub-menu keys like "solver:problems" — the parent
   * module ("solver") must be present in the menus list.
   */
  function hasMenu(menuKey: string): boolean {
    if (!menus.value.length) return true // no restriction = show all
    // exact match or parent match
    return menus.value.includes(menuKey) ||
           menus.value.includes(menuKey.split(':')[0])
  }

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.token
    localStorage.setItem('token', res.token)
    if (res.refreshToken) {
      localStorage.setItem('refreshToken', res.refreshToken)
    }
    await getUserInfo()
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    router.push('/login')
  }

  async function getUserInfo() {
    try {
      const info = await authApi.getUserInfo()
      userInfo.value = info
    } catch {
      logout()
    }
  }

  return {
    token,
    userInfo,
    menus,
    isAdmin,
    isViewer,
    canEditAlgorithm,
    hasMenu,
    login,
    logout,
    getUserInfo,
  }
})
