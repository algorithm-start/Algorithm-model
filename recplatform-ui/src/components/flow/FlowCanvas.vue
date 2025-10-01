<template>
  <div class="flow-canvas">
    <VueFlow
      v-model:nodes="nodes"
      v-model:edges="edges"
      :default-viewport="{ zoom: 1, x: 0, y: 0 }"
      :min-zoom="0.2"
      :max-zoom="2"
      fit-view-on-init
      @node-click="onNodeClick"
      @pane-click="onPaneClick"
      @connect="onConnect"
      @node-drag-start="onNodeDragStart"
    >
      <Background :gap="20" />
      <Controls />
      <template #node-serviceCall="nodeProps">
        <FlowNode v-bind="nodeProps" color="#409EFF" icon="Service" />
      </template>
      <template #node-solverInvoke="nodeProps">
        <FlowNode v-bind="nodeProps" color="#67C23A" icon="Solver" />
      </template>
      <template #node-dataTransform="nodeProps">
        <FlowNode v-bind="nodeProps" color="#E6A23C" icon="Transform" />
      </template>
      <template #node-decision="nodeProps">
        <FlowNode v-bind="nodeProps" color="#F56C6C" icon="Decision" />
      </template>
      <template #node-loop="nodeProps">
        <FlowNode v-bind="nodeProps" color="#909399" icon="Loop" />
      </template>
      <template #node-script="nodeProps">
        <FlowNode v-bind="nodeProps" color="#9B59B6" icon="Script" />
      </template>
      <template #node-start="nodeProps">
        <FlowNode v-bind="nodeProps" color="#67C23A" icon="Start" />
      </template>
      <template #node-end="nodeProps">
        <FlowNode v-bind="nodeProps" color="#F56C6C" icon="End" />
      </template>
      <template #node-dbQuery="nodeProps">
        <FlowNode v-bind="nodeProps" color="#409EFF" icon="DB Query" />
      </template>
      <template #node-apiCall="nodeProps">
        <FlowNode v-bind="nodeProps" color="#409EFF" icon="API Call" />
      </template>
      <template #node-fileRead="nodeProps">
        <FlowNode v-bind="nodeProps" color="#409EFF" icon="File Read" />
      </template>
      <template #node-filter="nodeProps">
        <FlowNode v-bind="nodeProps" color="#E6A23C" icon="Filter" />
      </template>
      <template #node-map="nodeProps">
        <FlowNode v-bind="nodeProps" color="#E6A23C" icon="Map" />
      </template>
      <template #node-join="nodeProps">
        <FlowNode v-bind="nodeProps" color="#E6A23C" icon="Join" />
      </template>
      <template #node-aggregate="nodeProps">
        <FlowNode v-bind="nodeProps" color="#E6A23C" icon="Aggregate" />
      </template>
      <template #node-dbWrite="nodeProps">
        <FlowNode v-bind="nodeProps" color="#67C23A" icon="DB Write" />
      </template>
      <template #node-apiPush="nodeProps">
        <FlowNode v-bind="nodeProps" color="#67C23A" icon="API Push" />
      </template>
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import FlowNode from './FlowNode.vue'
import type { Connection, GraphNode } from '@vue-flow/core'

const nodes = defineModel('nodes', { type: Array, default: () => [] })
const edges = defineModel('edges', { type: Array, default: () => [] })

const emit = defineEmits<{
  (e: 'nodeClick', node: GraphNode): void
  (e: 'paneClick'): void
  (e: 'beforeChange'): void
}>()

function onNodeClick(event: { node: GraphNode }) {
  emit('nodeClick', event.node)
}

function onPaneClick() {
  emit('paneClick')
}

let dragSnapshotTaken = false
function onNodeDragStart() {
  if (dragSnapshotTaken) return
  dragSnapshotTaken = true
  emit('beforeChange')
  // Allow the next drag gesture to snapshot again once this one settles.
  window.setTimeout(() => {
    dragSnapshotTaken = false
  }, 300)
}

function onConnect(params: Connection) {
  emit('beforeChange')
  edges.value = [
    ...edges.value,
    {
      id: `e-${params.source}-${params.target}`,
      source: params.source,
      target: params.target,
      type: 'smoothstep',
      animated: true,
    },
  ] as any[]
}
</script>

<style scoped>
.flow-canvas {
  width: 100%;
  height: 100%;
}
</style>
