<template>
  <el-container class="main-layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="sidebar-container">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="app-header" height="56px">
        <Header />
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const appStore = useAppStore()
</script>

<style scoped>
.main-layout {
  height: 100vh;
  overflow: hidden;
}
.sidebar-container {
  transition: width 0.3s;
  overflow: hidden;
  background-color: var(--layout-sidebar-bg);
  border-right: 1px solid #eef0f3;
}
.app-header {
  padding: 0;
  background-color: var(--layout-header-bg);
  border-bottom: 1px solid #eef0f3;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  display: flex;
  align-items: center;
}
.app-main {
  background-color: var(--layout-main-bg);
  overflow-y: auto;
  padding: 20px;
}
</style>
