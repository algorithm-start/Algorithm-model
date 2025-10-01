<template>
  <span class="field-label">
    {{ label }}
    <el-tooltip v-if="help" :content="help" placement="top" effect="dark" :show-after="150">
      <el-icon class="field-help-icon"><QuestionFilled /></el-icon>
    </el-tooltip>
  </span>
</template>

<script setup lang="ts">
import { QuestionFilled } from '@element-plus/icons-vue'

defineProps<{
  label: string
  help?: string
}>()
</script>

<style scoped>
.field-label {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.field-help-icon {
  color: var(--el-text-color-placeholder);
  cursor: help;
  font-size: 13px;
}
.field-help-icon:hover {
  color: var(--el-color-primary);
}
</style>
