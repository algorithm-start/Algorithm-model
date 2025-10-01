<template>
  <el-tag :type="tagType" :effect="effect" :size="size">
    {{ displayLabel }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  status: string
  label?: string
  effect?: 'dark' | 'light' | 'plain'
  size?: 'large' | 'default' | 'small'
}>(), {
  effect: 'light',
  size: 'default',
})

const statusTypeMap: Record<string, string> = {
  DRAFT: 'info',
  PENDING: 'warning',
  RUNNING: 'warning',
  SOLVING: 'warning',
  SOLVED: 'success',
  FAILED: 'danger',
  OPTIMAL: 'success',
  INFEASIBLE: 'danger',
  IDLE: 'info',
  ACTIVE: 'success',
  EXECUTING: 'warning',
  COMPLETED: 'success',
  ERROR: 'danger',
  CANCELLED: 'info',
  STOPPED: 'info',
  PUBLISHED: 'success',
  CONNECTED: 'success',
  DISCONNECTED: 'danger',
  SUCCESS: 'success',
  WARNING: 'warning',
}

const tagType = computed(() => {
  return (statusTypeMap[props.status?.toUpperCase()] || 'info') as 'success' | 'warning' | 'danger' | 'info'
})

const displayLabel = computed(() => {
  if (props.label) return props.label
  // 尝试获取国际化标签
  const key = `status.${props.status?.toLowerCase()}`
  const translated = t(key)
  return translated !== key ? translated : props.status
})
</script>
