<template>
  <div class="sidebar">
    <div class="logo">
      <span class="logo-mark">AE</span>
      <span v-show="!appStore.sidebarCollapsed" class="logo-text">AEPlatform</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :collapse-transition="false"
      router
      class="sidebar-menu"
    >
      <el-menu-item index="/dashboard">
        <el-icon><Odometer /></el-icon>
        <template #title>{{ t('menu.dashboard') }}</template>
      </el-menu-item>

      <el-sub-menu v-if="userStore.hasMenu('solver')" index="/solver">
        <template #title>
          <el-icon><Cpu /></el-icon>
          <span>{{ t('menu.solver') }}</span>
        </template>
        <el-menu-item index="/solver/problems">{{ t('menu.problems') }}</el-menu-item>
        <el-menu-item index="/solver/algorithms">{{ t('menu.algorithms') }}</el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="userStore.hasMenu('orchestrator')" index="/orchestrator">
        <template #title>
          <el-icon><Connection /></el-icon>
          <span>{{ t('menu.orchestrator') }}</span>
        </template>
        <el-menu-item index="/orchestrator/flows">{{ t('menu.flows') }}</el-menu-item>
        <el-menu-item index="/orchestrator/executions">{{ t('menu.executions') }}</el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="userStore.hasMenu('data')" index="/data">
        <template #title>
          <el-icon><DataAnalysis /></el-icon>
          <span>{{ t('menu.data') }}</span>
        </template>
        <el-menu-item index="/data/sources">{{ t('menu.sources') }}</el-menu-item>
        <el-menu-item index="/data/pipelines">{{ t('menu.pipelines') }}</el-menu-item>
        <el-menu-item index="/data/apis">{{ t('menu.apis') }}</el-menu-item>
        <el-menu-item index="/data/query">{{ t('menu.query') }}</el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="userStore.hasMenu('admin')" index="/admin">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>{{ t('menu.admin') }}</span>
        </template>
        <el-menu-item index="/admin/users">{{ t('menu.users') }}</el-menu-item>
        <el-menu-item index="/admin/roles">{{ t('menu.roles') }}</el-menu-item>
        <el-menu-item index="/admin/workspaces">{{ t('menu.workspaces') }}</el-menu-item>
        <el-menu-item index="/admin/audit">{{ t('menu.audit') }}</el-menu-item>
        <el-menu-item index="/admin/system">{{ t('menu.system') }}</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { Odometer, Cpu, Connection, DataAnalysis, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()
const { t } = useI18n()

const activeMenu = computed(() => {
  return route.path
})
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #eef0f3;
}
.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8f1f 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  flex-shrink: 0;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
  white-space: nowrap;
  letter-spacing: 0.5px;
}
.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding-top: 8px;
}
</style>
