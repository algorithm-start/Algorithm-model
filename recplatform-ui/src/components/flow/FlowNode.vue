<template>
  <div class="flow-node" :style="{ borderColor: color }">
    <Handle type="target" :position="Position.Left" />
    <div class="node-header" :style="{ backgroundColor: color }">
      <span class="node-icon">{{ icon }}</span>
    </div>
    <div class="node-label">{{ nodeLabel }}</div>
    <Handle type="source" :position="Position.Right" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = withDefaults(defineProps<{
  color?: string
  icon?: string
  label?: string
  data?: Record<string, any>
}>(), {
  color: '#409EFF',
  icon: '',
  label: '',
})

const nodeLabel = computed(() => {
  return props.data?.label || props.label || 'Node'
})
</script>

<style scoped>
.flow-node {
  background: #fff;
  border: 2px solid #ddd;
  border-radius: 8px;
  min-width: 140px;
  overflow: hidden;
}
.node-header {
  padding: 4px 8px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.node-icon {
  font-size: 11px;
}
.node-label {
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  text-align: center;
  white-space: nowrap;
}
</style>
