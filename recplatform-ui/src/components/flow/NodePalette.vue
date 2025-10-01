<template>
  <div class="node-palette">
    <div class="palette-title">{{ t('orchestrator.nodeTypes') }}</div>
    <div class="palette-section" v-for="section in sections" :key="section.label">
      <div class="section-label">{{ section.label }}</div>
      <div
        v-for="node in section.nodes"
        :key="node.type"
        class="palette-item"
        draggable="true"
        :style="{ borderLeftColor: node.color }"
        @dragstart="onDragStart($event, node)"
      >
        <span class="palette-item-icon" :style="{ color: node.color }">{{ node.icon }}</span>
        <span class="palette-item-label">{{ node.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

export interface NodeTypeItem {
  type: string
  label: string
  icon: string
  color: string
}

export interface NodeSection {
  label: string
  nodes: NodeTypeItem[]
}

defineProps<{
  sections: NodeSection[]
}>()

function onDragStart(event: DragEvent, node: NodeTypeItem) {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/vueflow', JSON.stringify(node))
    event.dataTransfer.effectAllowed = 'move'
  }
}
</script>

<style scoped>
.node-palette {
  padding: 12px;
  width: 200px;
  background: #fff;
  border-right: 1px solid var(--el-border-color-light);
  overflow-y: auto;
}
.palette-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}
.palette-section {
  margin-bottom: 16px;
}
.section-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 4px;
  border-radius: 6px;
  border-left: 3px solid #ddd;
  background: #f9f9fb;
  cursor: grab;
  transition: all 0.2s;
}
.palette-item:hover {
  background: #f0f2f5;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.palette-item-icon {
  font-size: 12px;
  font-weight: 700;
  width: 20px;
  text-align: center;
}
.palette-item-label {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
