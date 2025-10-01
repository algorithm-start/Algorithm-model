<template>
  <div class="header">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <!-- Workspace Switcher -->
      <el-dropdown trigger="click" @command="handleWorkspaceChange" class="workspace-switcher" v-if="workspaceStore.workspaces.length > 0">
        <span class="workspace-button">
          <el-icon><OfficeBuilding /></el-icon>
          <span class="workspace-text">{{ workspaceStore.currentWorkspace?.name || '选择空间' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="ws in workspaceStore.workspaces"
              :key="ws.id"
              :command="ws.id"
              :class="{ 'is-active': workspaceStore.currentWorkspaceId === ws.id }"
            >
              <el-icon v-if="workspaceStore.currentWorkspaceId === ws.id" class="check-icon"><Check /></el-icon>
              <span :class="{ 'ml-16': workspaceStore.currentWorkspaceId !== ws.id }">{{ ws.name }}</span>
              <el-tag v-if="ws.myRole === 'ADMIN'" size="small" type="warning" style="margin-left: 8px">管理员</el-tag>
            </el-dropdown-item>
            <el-dropdown-item divided command="__manage__">
              <el-icon><Setting /></el-icon>
              <span>管理空间</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- Language Switcher -->
      <el-dropdown trigger="click" @command="handleLanguageChange" class="lang-switcher">
        <span class="lang-button">
          <el-icon><Switch /></el-icon>
          <span class="lang-text">{{ currentLanguageLabel }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item 
              v-for="lang in languages" 
              :key="lang.value" 
              :command="lang.value"
              :class="{ 'is-active': locale === lang.value }"
            >
              <el-icon v-if="locale === lang.value" class="check-icon"><Check /></el-icon>
              <span :class="{ 'ml-16': locale !== lang.value }">{{ lang.label }}</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- User Dropdown -->
      <el-dropdown trigger="click" @command="handleCommand" class="user-dropdown">
        <span class="user-info">
          <el-avatar :size="32" :src="userStore.userInfo?.avatar">
            {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>
          <span class="username">{{ userStore.userInfo?.username || 'User' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">{{ t('common.profile') }}</el-dropdown-item>
            <el-dropdown-item command="logout" divided>{{ t('common.logout') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useWorkspaceStore } from '@/stores/workspace'
import { Fold, Expand, ArrowDown, Switch, Check, OfficeBuilding, Setting } from '@element-plus/icons-vue'
import type { Language } from '@/i18n'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const workspaceStore = useWorkspaceStore()

onMounted(() => {
  if (userStore.token) {
    workspaceStore.loadWorkspaces()
  }
})

function handleWorkspaceChange(command: string) {
  if (command === '__manage__') {
    router.push('/admin/workspaces')
    return
  }
  workspaceStore.setCurrentWorkspace(command)
  // Reload current page to apply new workspace filter
  router.go(0)
}

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  return matched.map(item => ({
    path: item.path,
    title: t(item.meta.title as string),
  }))
})

// Language switching
const languages = [
  { value: 'zh-CN', label: '中文' },
  { value: 'en-US', label: 'English' },
]

const currentLanguageLabel = computed(() => {
  const lang = languages.find(l => l.value === locale.value)
  return lang?.label || '中文'
})

function handleLanguageChange(lang: Language) {
  locale.value = lang
  localStorage.setItem('locale', lang)
}

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
  } else if (command === 'profile') {
    router.push('/admin/users')
  }
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}
.collapse-btn:hover {
  color: var(--el-color-primary);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}
.username {
  font-size: 14px;
}
.workspace-switcher {
  margin-right: 8px;
}
.workspace-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  cursor: pointer;
  color: var(--el-text-color-regular);
  border-radius: 4px;
  transition: all 0.3s;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-light);
}
.workspace-button:hover {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
  color: var(--el-color-primary);
}
.workspace-text {
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lang-switcher {
  margin-right: 8px;
}
.lang-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  cursor: pointer;
  color: var(--el-text-color-regular);
  border-radius: 4px;
  transition: all 0.3s;
}
.lang-button:hover {
  background-color: var(--el-fill-color-light);
  color: var(--el-color-primary);
}
.lang-text {
  font-size: 14px;
}
.check-icon {
  color: var(--el-color-primary);
  margin-right: 4px;
}
.ml-16 {
  margin-left: 16px;
}
.user-dropdown {
  margin-left: 8px;
}
</style>
