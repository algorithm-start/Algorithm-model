<template>
  <div class="flow-editor">
    <div class="flow-toolbar">
      <div class="toolbar-left">
        <el-input v-model="flowName" :placeholder="t('form.flowName')" style="width: 240px" />
        <el-input v-model="flowDescription" :placeholder="t('form.flowDescription')" style="width: 300px" />
      </div>
      <div class="toolbar-right">
        <el-button :disabled="!canUndo" @click="handleUndo">
          <el-icon><RefreshLeft /></el-icon> {{ t('common.undo') }}
        </el-button>
        <el-button @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon>{{ t('common.save') }}</el-button>
        <el-button type="success" @click="handlePublish" :loading="publishing">
          <el-icon><Upload /></el-icon> {{ t('common.publish') }}
        </el-button>
        <el-button type="warning" @click="handleRun" :loading="running">
          <el-icon><VideoPlay /></el-icon> {{ t('common.run') }}
        </el-button>
      </div>
    </div>

    <div class="flow-body">
      <NodePalette :sections="nodeSections" />

      <div
        class="flow-canvas-wrapper"
        @dragover.prevent
        @drop="onDrop"
      >
        <FlowCanvas
          v-model:nodes="nodes"
          v-model:edges="edges"
          @node-click="onNodeClick"
          @pane-click="onPaneClick"
          @before-change="pushHistory"
        />
      </div>

      <div class="properties-panel" v-if="selectedNode">
        <div class="panel-header">
          <span>{{ t('form.properties') }}</span>
          <el-button link @click="selectedNode = null">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="panel-body">
          <el-form label-width="92px" label-position="top" size="small">
            <el-form-item>
              <template #label>
                <FieldLabel :label="t('form.label')" :help="t('flowHelp.label')" />
              </template>
              <el-input v-model="selectedNode.data.label" :placeholder="t('flowHelp.labelPlaceholder')" />
            </el-form-item>
            <el-form-item :label="t('table.type')">
              <el-tag size="small">{{ selectedNode.type }}</el-tag>
            </el-form-item>

            <template v-if="selectedNode.type === 'serviceCall'">
              <el-form-item>
                <template #label>
                  <FieldLabel label="URL" :help="t('flowHelp.url')" />
                </template>
                <el-input v-model="selectedNode.data.config.url" :placeholder="t('flowHelp.urlPlaceholder')" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel label="Method" :help="t('flowHelp.method')" />
                </template>
                <el-select v-model="selectedNode.data.config.method" :placeholder="t('flowHelp.methodPlaceholder')" style="width: 100%">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.requestBody')" :help="t('flowHelp.requestBody')" />
                </template>
                <el-input
                  v-model="serviceBodyText"
                  type="textarea"
                  :rows="4"
                  :placeholder="t('flowHelp.requestBodyPlaceholder')"
                />
                <span v-if="serviceBodyError" class="field-unit" style="color: var(--el-color-danger)">{{ serviceBodyError }}</span>
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'solverInvoke'">
              <el-form-item>
                <template #label>
                  <FieldLabel label="Problem ID" :help="t('flowHelp.problemId')" />
                </template>
                <el-input v-model="selectedNode.data.config.problemId" :placeholder="t('flowHelp.problemIdPlaceholder')" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.algorithm')" :help="t('flowHelp.algorithm')" />
                </template>
                <el-select v-model="selectedNode.data.config.algorithm" :placeholder="t('flowHelp.algorithmPlaceholder')" clearable style="width: 100%">
                  <el-option label="MILP (混合整数规划)" value="MILP" />
                  <el-option label="OR-Tools" value="ortools" />
                  <el-option label="遗传算法" value="genetic" />
                  <el-option label="模拟退火" value="annealing" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.autoSolve')" :help="t('flowHelp.autoSolve')" />
                </template>
                <el-switch v-model="selectedNode.data.config.autoSolve" />
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'dataTransform'">
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.operation')" :help="t('flowHelp.operation')" />
                </template>
                <el-select v-model="selectedNode.data.config.operation" :placeholder="t('flowHelp.operationPlaceholder')" style="width: 100%">
                  <el-option :label="t('form.opMap')" value="map" />
                  <el-option :label="t('form.opFilter')" value="filter" />
                  <el-option :label="t('form.opAggregate')" value="aggregate" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.inputField')" :help="t('flowHelp.inputField')" />
                </template>
                <el-input v-model="selectedNode.data.config.inputField" :placeholder="t('flowHelp.inputFieldPlaceholder')" />
              </el-form-item>

              <template v-if="selectedNode.data.config.operation === 'map'">
                <el-form-item>
                  <template #label>
                    <FieldLabel :label="t('form.keepFields')" :help="t('flowHelp.keepFields')" />
                  </template>
                  <el-select
                    v-model="selectedNode.data.config.fields"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    :placeholder="t('flowHelp.keepFieldsPlaceholder')"
                    style="width: 100%"
                  />
                </el-form-item>
              </template>

              <template v-if="selectedNode.data.config.operation === 'filter'">
                <el-form-item>
                  <template #label>
                    <FieldLabel :label="t('form.filterField')" :help="t('flowHelp.filterField')" />
                  </template>
                  <el-input v-model="selectedNode.data.config.field" :placeholder="t('flowHelp.filterFieldPlaceholder')" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <FieldLabel :label="t('form.filterValue')" :help="t('flowHelp.filterValue')" />
                  </template>
                  <el-input v-model="selectedNode.data.config.value" :placeholder="t('flowHelp.filterValuePlaceholder')" />
                </el-form-item>
              </template>

              <template v-if="selectedNode.data.config.operation === 'aggregate'">
                <el-form-item>
                  <template #label>
                    <FieldLabel :label="t('form.aggregateOp')" :help="t('flowHelp.aggregateOp')" />
                  </template>
                  <el-select v-model="selectedNode.data.config.aggregateOp" :placeholder="t('flowHelp.aggregateOpPlaceholder')" style="width: 100%">
                    <el-option :label="t('form.aggCount')" value="count" />
                    <el-option :label="t('form.aggSum')" value="sum" />
                    <el-option :label="t('form.aggAvg')" value="avg" />
                    <el-option :label="t('form.aggMin')" value="min" />
                    <el-option :label="t('form.aggMax')" value="max" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="selectedNode.data.config.aggregateOp && selectedNode.data.config.aggregateOp !== 'count'">
                  <template #label>
                    <FieldLabel :label="t('form.aggregateField')" :help="t('flowHelp.aggregateField')" />
                  </template>
                  <el-input v-model="selectedNode.data.config.field" :placeholder="t('flowHelp.aggregateFieldPlaceholder')" />
                </el-form-item>
              </template>
            </template>

            <template v-if="selectedNode.type === 'decision'">
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.condition')" :help="t('flowHelp.condition')" />
                </template>
                <el-input v-model="selectedNode.data.config.condition" :placeholder="t('flowHelp.conditionPlaceholder')" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.trueBranch')" :help="t('flowHelp.trueBranch')" />
                </template>
                <el-select
                  v-model="selectedNode.data.config.trueBranch"
                  :placeholder="downstreamNodes.length ? t('flowHelp.selectTargetNode') : t('flowHelp.noDownstream')"
                  :no-data-text="t('flowHelp.noDownstream')"
                  clearable
                  style="width: 100%"
                >
                  <el-option v-for="n in downstreamNodes" :key="n.id" :label="n.label" :value="n.id" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.falseBranch')" :help="t('flowHelp.falseBranch')" />
                </template>
                <el-select
                  v-model="selectedNode.data.config.falseBranch"
                  :placeholder="downstreamNodes.length ? t('flowHelp.selectTargetNode') : t('flowHelp.noDownstream')"
                  :no-data-text="t('flowHelp.noDownstream')"
                  clearable
                  style="width: 100%"
                >
                  <el-option v-for="n in downstreamNodes" :key="n.id" :label="n.label" :value="n.id" />
                </el-select>
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'loop'">
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.loopType')" :help="t('flowHelp.loopType')" />
                </template>
                <el-select v-model="selectedNode.data.config.loopType" :placeholder="t('flowHelp.loopTypePlaceholder')" style="width: 100%">
                  <el-option :label="t('form.forEach')" value="FOREACH" />
                  <el-option label="While" value="WHILE" />
                  <el-option :label="t('form.count')" value="COUNT" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.maxIterations')" :help="t('flowHelp.maxIterations')" />
                </template>
                <el-input-number
                  v-model="selectedNode.data.config.maxIterations"
                  :min="1"
                  :max="100000"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.loopBody')" :help="t('flowHelp.loopBody')" />
                </template>
                <el-select
                  v-model="selectedNode.data.config.loopBody"
                  :placeholder="downstreamNodes.length ? t('flowHelp.selectTargetNode') : t('flowHelp.noDownstream')"
                  :no-data-text="t('flowHelp.noDownstream')"
                  clearable
                  style="width: 100%"
                >
                  <el-option v-for="n in downstreamNodes" :key="n.id" :label="n.label" :value="n.id" />
                </el-select>
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'script'">
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.language')" :help="t('flowHelp.language')" />
                </template>
                <el-select v-model="selectedNode.data.config.language" :placeholder="t('flowHelp.languagePlaceholder')" style="width: 100%">
                  <el-option label="JavaScript" value="javascript" />
                  <el-option label="Python" value="python" />
                  <el-option label="Groovy" value="groovy" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <template #label>
                  <FieldLabel :label="t('form.script')" :help="t('flowHelp.script')" />
                </template>
                <el-input
                  v-model="selectedNode.data.config.script"
                  type="textarea"
                  :rows="6"
                  :placeholder="t('flowHelp.scriptPlaceholder')"
                />
              </el-form-item>
            </template>

            <el-divider />
            <el-button type="danger" size="small" @click="deleteSelectedNode">
              {{ t('common.delete') }}
            </el-button>
          </el-form>
        </div>
      </div>
      <div class="properties-panel properties-panel-empty" v-else>
        <el-empty :description="t('form.selectNodeToEdit')" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Upload, VideoPlay, Close, RefreshLeft } from '@element-plus/icons-vue'
import NodePalette from '@/components/flow/NodePalette.vue'
import FlowCanvas from '@/components/flow/FlowCanvas.vue'
import FieldLabel from '@/components/flow/FieldLabel.vue'
import { orchestratorApi } from '@/api/orchestrator'
import type { NodeSection } from '@/components/flow/NodePalette.vue'
import type { GraphNode } from '@vue-flow/core'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const flowName = ref('')
const flowDescription = ref('')
const saving = ref(false)
const publishing = ref(false)
const running = ref(false)
const selectedNode = ref<GraphNode | null>(null)

const nodes = ref<any[]>([])
const edges = ref<any[]>([])

// serviceCall request body is edited as JSON text but stored as an object in
// config.body (the backend expects a Map and substitutes ${var} references).
const serviceBodyError = ref('')
const serviceBodyText = computed<string>({
  get() {
    const body = selectedNode.value?.data?.config?.body
    if (body === undefined || body === null) return ''
    if (typeof body === 'string') return body
    try {
      return JSON.stringify(body, null, 2)
    } catch {
      return ''
    }
  },
  set(text: string) {
    if (!selectedNode.value) return
    const config = selectedNode.value.data.config
    const trimmed = text.trim()
    if (!trimmed) {
      delete config.body
      serviceBodyError.value = ''
      return
    }
    try {
      config.body = JSON.parse(trimmed)
      serviceBodyError.value = ''
    } catch {
      serviceBodyError.value = t('flowHelp.requestBodyInvalid')
    }
  },
})

const flowId = route.params.id as string | undefined

// --- Undo history ---
// Each entry is a deep snapshot of the canvas (nodes + edges) taken right
// before a mutating action (drop / delete / connect / drag). Undo pops the
// last snapshot and restores it.
const historyStack = ref<Array<{ nodes: any[]; edges: any[] }>>([])
const MAX_HISTORY = 50
const canUndo = computed(() => historyStack.value.length > 0)

function pushHistory() {
  historyStack.value.push({
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    edges: JSON.parse(JSON.stringify(edges.value)),
  })
  if (historyStack.value.length > MAX_HISTORY) {
    historyStack.value.shift()
  }
}

function handleUndo() {
  const snapshot = historyStack.value.pop()
  if (!snapshot) return
  nodes.value = snapshot.nodes
  edges.value = snapshot.edges
  selectedNode.value = null
  ElMessage.success(t('message.undoDone'))
}

function onUndoKeydown(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z') {
    event.preventDefault()
    handleUndo()
  }
}

const nodeSections = computed<NodeSection[]>(() => [
  {
    label: t('orchestrator.control'),
    nodes: [
      { type: 'start', label: t('orchestrator.start'), icon: '▶', color: '#67C23A' },
      { type: 'end', label: t('orchestrator.end'), icon: '■', color: '#F56C6C' },
      { type: 'decision', label: t('orchestrator.decision'), icon: '◇', color: '#E6A23C' },
      { type: 'loop', label: t('orchestrator.loop'), icon: '↻', color: '#909399' },
    ],
  },
  {
    label: t('orchestrator.actions'),
    nodes: [
      { type: 'serviceCall', label: t('orchestrator.serviceCall'), icon: '⚡', color: '#409EFF' },
      { type: 'solverInvoke', label: t('orchestrator.solverInvoke'), icon: '⚙', color: '#67C23A' },
      { type: 'dataTransform', label: t('orchestrator.dataTransform'), icon: '🔄', color: '#E6A23C' },
      { type: 'script', label: t('orchestrator.script'), icon: '📝', color: '#9B59B6' },
    ],
  },
])

let nodeCounter = 0

// Backend persists node types as the FlowNodeType enum (UPPER_SNAKE), but the
// canvas registers node components by camelCase keys. Map between them so flows
// saved by the backend (or seeded demo data) render their nodes correctly.
const BACKEND_TO_CANVAS_TYPE: Record<string, string> = {
  START: 'start',
  END: 'end',
  DECISION_GATE: 'decision',
  LOOP: 'loop',
  SERVICE_CALL: 'serviceCall',
  SOLVER_INVOKE: 'solverInvoke',
  DATA_TRANSFORM: 'dataTransform',
  SCRIPT: 'script',
}

function toCanvasNodeType(rawType: string | undefined): string {
  if (!rawType) return 'serviceCall'
  return BACKEND_TO_CANVAS_TYPE[rawType] ?? rawType
}

const CANVAS_TO_BACKEND_TYPE: Record<string, string> = Object.fromEntries(
  Object.entries(BACKEND_TO_CANVAS_TYPE).map(([backend, canvas]) => [canvas, backend]),
)

function toBackendNodeType(canvasType: string | undefined): string {
  if (!canvasType) return 'SERVICE_CALL'
  return CANVAS_TO_BACKEND_TYPE[canvasType] ?? canvasType
}

/**
 * All nodes reachable from the selected node by following edges forwards.
 *
 * <p>Decision branch and loop-body dropdowns offer downstream nodes (those the
 * current node can route to), since a branch must point at a node that comes
 * after the decision in the flow.
 */
const downstreamNodes = computed<Array<{ id: string; label: string; type: string }>>(() => {
  if (!selectedNode.value) return []
  const sourceId = selectedNode.value.id

  const outgoingBySource = new Map<string, string[]>()
  for (const edge of edges.value) {
    const list = outgoingBySource.get(edge.source) || []
    list.push(edge.target)
    outgoingBySource.set(edge.source, list)
  }

  const visited = new Set<string>()
  const queue = [...(outgoingBySource.get(sourceId) || [])]
  while (queue.length > 0) {
    const current = queue.shift() as string
    if (visited.has(current)) continue
    visited.add(current)
    for (const target of outgoingBySource.get(current) || []) {
      if (!visited.has(target)) queue.push(target)
    }
  }

  return nodes.value
    .filter((node) => visited.has(node.id))
    .map((node) => ({
      id: node.id,
      label: node.data?.label || node.id,
      type: node.type,
    }))
})

function onDrop(event: DragEvent) {
  const data = event.dataTransfer?.getData('application/vueflow')
  if (!data) return

  const nodeType = JSON.parse(data)
  const canvasEl = (event.target as HTMLElement).closest('.flow-canvas-wrapper')
  if (!canvasEl) return

  const rect = canvasEl.getBoundingClientRect()
  const x = event.clientX - rect.left - 70
  const y = event.clientY - rect.top - 20

  pushHistory()
  nodeCounter++
  const newNode = {
    id: `node-${nodeCounter}`,
    type: nodeType.type,
    position: { x, y },
    data: {
      label: nodeType.label,
      config: {} as Record<string, any>,
    },
  }

  nodes.value = [...nodes.value, newNode]
}

function onNodeClick(node: GraphNode) {
  selectedNode.value = node
}

function onPaneClick() {
  selectedNode.value = null
}

function deleteSelectedNode() {
  if (!selectedNode.value) return
  pushHistory()
  const nodeId = selectedNode.value.id
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
  selectedNode.value = null
}

async function handleSave() {
  saving.value = true
  try {
    // Backend expects nodes/edges nested under `definition` with enum node
    // types; map the canvas shape back to that contract.
    const payload = {
      name: flowName.value || t('orchestrator.untitledFlow'),
      description: flowDescription.value || undefined,
      definition: {
        nodes: nodes.value.map(n => ({
          id: n.id,
          type: toBackendNodeType(n.type),
          name: n.data.label,
          config: n.data.config || {},
          position: n.position,
        })),
        edges: edges.value.map(e => ({
          id: e.id,
          source: e.source,
          target: e.target,
        })),
      },
    }

    if (flowId) {
      await orchestratorApi.updateFlow(flowId, payload as any)
    } else {
      const res = await orchestratorApi.createFlow(payload as any)
      const newId = (res as any).id
      if (newId) {
        router.replace(`/orchestrator/editor/${newId}`)
      }
    }
    ElMessage.success(t('message.flowSaved'))
  } catch {
    // Error handled by interceptor
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  publishing.value = true
  try {
    await handleSave()
    if (flowId) {
      await orchestratorApi.publishFlow(flowId)
      ElMessage.success(t('message.flowPublished'))
    }
  } catch {
    // Error handled
  } finally {
    publishing.value = false
  }
}

function validateNodeConfigs(): string[] {
  const errors: string[] = []
  for (const node of nodes.value) {
    const config = node.data?.config || {}
    const label = node.data?.label || node.id
    switch (node.type) {
      case 'serviceCall':
        if (!config.url?.trim()) errors.push(`"${label}": ${t('flowValidate.missingUrl')}`)
        break
      case 'solverInvoke':
        if (!config.problemId?.trim()) errors.push(`"${label}": ${t('flowValidate.missingProblemId')}`)
        break
      case 'dataTransform':
        if (!config.operation) {
          errors.push(`"${label}": ${t('flowValidate.missingOperation')}`)
        } else if (config.operation === 'filter' && !config.field?.trim()) {
          errors.push(`"${label}": ${t('flowValidate.missingFilterField')}`)
        } else if (config.operation === 'aggregate' && config.aggregateOp && config.aggregateOp !== 'count' && !config.field?.trim()) {
          errors.push(`"${label}": ${t('flowValidate.missingAggregateField')}`)
        }
        break
      case 'decision':
        if (!config.condition?.trim()) errors.push(`"${label}": ${t('flowValidate.missingCondition')}`)
        break
      case 'script':
        if (!config.script?.trim()) errors.push(`"${label}": ${t('flowValidate.missingScript')}`)
        break
      case 'loop':
        if (!config.loopType) errors.push(`"${label}": ${t('flowValidate.missingLoopType')}`)
        break
    }
  }
  return errors
}

async function handleRun() {
  if (!flowId) {
    ElMessage.warning(t('message.pleaseSaveFlowFirst'))
    return
  }
  const errors = validateNodeConfigs()
  if (errors.length > 0) {
    ElMessage.error({
      message: `节点配置不完整，无法执行：<br/>${errors.join('<br/>')}`,
      duration: 5000,
      dangerouslyUseHTMLString: true,
    })
    return
  }
  running.value = true
  try {
    // Auto save + publish before execution
    await handleSave()
    if (flowId) {
      await orchestratorApi.publishFlow(flowId)
      await orchestratorApi.executeFlow(flowId)
      ElMessage.success(t('message.flowExecutionStarted'))
      router.push('/orchestrator/executions')
    }
  } catch {
    // Error handled by interceptor
  } finally {
    running.value = false
  }
}

onMounted(async () => {
  window.addEventListener('keydown', onUndoKeydown)
  if (flowId) {
    try {
      const res = await orchestratorApi.getFlow(flowId)
      const flow = res as any
      flowName.value = flow.name || ''
      flowDescription.value = flow.description || ''
      // Nodes/edges live under definition in the backend VO; fall back to the
      // flat shape for older payloads.
      const rawNodes = flow.definition?.nodes || flow.nodes || []
      const rawEdges = flow.definition?.edges || flow.edges || []
      nodes.value = rawNodes.map((n: any) => ({
        id: n.id,
        type: toCanvasNodeType(n.type),
        position: n.position || { x: 0, y: 0 },
        data: { label: n.label || n.name || n.id, config: n.config || {} },
      }))
      edges.value = rawEdges.map((e: any) => ({
        id: e.id,
        source: e.source,
        target: e.target,
        type: 'smoothstep',
        animated: true,
      }))
      nodeCounter = nodes.value.length
    } catch {
      // Error handled
    }
  } else {
    // Add default start and end nodes
    nodes.value = [
      { id: 'node-start', type: 'start', position: { x: 100, y: 200 }, data: { label: 'Start', config: {} } },
      { id: 'node-end', type: 'end', position: { x: 600, y: 200 }, data: { label: 'End', config: {} } },
    ]
    nodeCounter = 2
  }
  // Initial load is not an undoable action.
  historyStack.value = []
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onUndoKeydown)
})
</script>

<style scoped>
.flow-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 96px);
  margin: -20px;
}
.flow-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid var(--el-border-color-light);
}
.toolbar-left {
  display: flex;
  gap: 12px;
}
.toolbar-right {
  display: flex;
  gap: 8px;
}
.flow-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.flow-canvas-wrapper {
  flex: 1;
  height: 100%;
}
.properties-panel {
  width: 280px;
  background: #fff;
  border-left: 1px solid var(--el-border-color-light);
  overflow-y: auto;
}
.properties-panel-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.panel-body {
  padding: 16px;
}
.field-unit {
  margin-left: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
