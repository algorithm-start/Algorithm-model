<template>
  <div class="page-header">
    <div class="page-header-left">
      <h2 class="page-title">{{ title }}</h2>
      <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
    </div>
    <div class="page-header-right">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.page-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
.page-header-right {
  display: flex;
  gap: 8px;
}
</style>
