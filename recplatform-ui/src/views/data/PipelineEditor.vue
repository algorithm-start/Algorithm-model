<template>
  <div class="pipeline-editor">
    <div class="pipeline-toolbar">
      <div class="toolbar-left">
        <el-input v-model="pipelineName" :placeholder="t('table.name')" style="width: 240px" />
        <el-input v-model="pipelineDescription" :placeholder="t('form.description')" style="width: 300px" />
      </div>
      <div class="toolbar-right">
        <el-button @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon>{{ t('common.save') }}</el-button>
        <el-button type="warning" @click="handleRun" :loading="running">
          <el-icon><VideoPlay /></el-icon> {{ t('common.run') }}
        </el-button>
      </div>
    </div>

    <div class="pipeline-body">
      <NodePalette :sections="nodeSections" />

      <div class="flow-canvas-wrapper" @dragover.prevent @drop="onDrop">
        <FlowCanvas
          v-model:nodes="nodes"
          v-model:edges="edges"
          @node-click="onNodeClick"
          @pane-click="onPaneClick"
        />
      </div>

      <div class="properties-panel" v-if="selectedNode">
        <div class="panel-header">
          <span>{{ t('form.properties') }}</span>
          <el-button link @click="selectedNode = null"><el-icon><Close /></el-icon></el-button>
        </div>
        <div class="panel-body">
          <el-form label-width="80px" size="small">
            <el-form-item :label="t('form.label')">
              <el-input v-model="selectedNode.data.label" />
            </el-form-item>
            <el-form-item :label="t('table.type')">
              <el-tag size="small">{{ selectedNode.type }}</el-tag>
            </el-form-item>
            <template v-if="selectedNode.type === 'dbQuery'">
              <el-form-item :label="t('form.dataSource')">
                <el-select v-model="selectedNode.data.config.sourceId" filterable
                  :placeholder="t('pipeline.selectDataSource')" style="width: 100%"
                  @change="onSourceChange">
                  <el-option v-for="src in dataSources" :key="src.id"
                    :label="`${src.name} (${src.type})`" :value="src.id" />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('pipeline.table')">
                <el-select v-model="selectedNode.data.config.table" filterable
                  :placeholder="t('pipeline.selectTable')" style="width: 100%"
                  :loading="tablesLoading" :no-data-text="t('pipeline.noTables')"
                  @change="onTableChange">
                  <el-option v-for="tbl in sourceTables" :key="tbl.name"
                    :label="tbl.name" :value="tbl.name">
                    <span>{{ tbl.name }}</span>
                    <span class="table-cols">{{ tbl.columns?.length || 0 }} {{ t('pipeline.columns') }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="SQL">
                <el-input v-model="selectedNode.data.config.sql" type="textarea" :rows="3" placeholder="SELECT * FROM ..." />
              </el-form-item>
            </template>
            <template v-if="selectedNode.type === 'filter'">
              <el-form-item :label="t('form.condition')">
                <el-input v-model="selectedNode.data.config.condition" :placeholder="t('pipeline.filterExpression')" />
              </el-form-item>
            </template>
            <template v-if="selectedNode.type === 'map'">
              <el-form-item :label="t('pipeline.mapping')">
                <el-input v-model="selectedNode.data.config.mapping" type="textarea" :rows="3" :placeholder="t('pipeline.fieldMappings')" />
              </el-form-item>
            </template>
            <template v-if="selectedNode.type === 'join'">
              <el-form-item :label="t('pipeline.joinType')">
                <el-select v-model="selectedNode.data.config.joinType">
                  <el-option label="Inner" value="INNER" />
                  <el-option label="Left" value="LEFT" />
                  <el-option label="Right" value="RIGHT" />
                  <el-option label="Full" value="FULL" />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('pipeline.joinKey')">
                <el-input v-model="selectedNode.data.config.joinKey" :placeholder="t('pipeline.joinKeyField')" />
              </el-form-item>
            </template>
            <template v-if="selectedNode.type === 'aggregate'">
              <el-form-item :label="t('pipeline.groupBy')">
                <el-input v-model="selectedNode.data.config.groupBy" :placeholder="t('pipeline.groupFields')" />
              </el-form-item>
              <el-form-item :label="t('pipeline.aggregation')">
                <el-input v-model="selectedNode.data.config.aggregation" placeholder="SUM(count), AVG(price)" />
              </el-form-item>
            </template>
            <template v-if="selectedNode.type === 'dbWrite'">
              <el-form-item :label="t('pipeline.target')">
                <el-input v-model="selectedNode.data.config.targetId" :placeholder="t('pipeline.targetDataSourceId')" />
              </el-form-item>
              <el-form-item :label="t('pipeline.table')">
                <el-input v-model="selectedNode.data.config.table" :placeholder="t('pipeline.targetTable')" />
              </el-form-item>
              <el-form-item :label="t('pipeline.mode')">
                <el-select v-model="selectedNode.data.config.writeMode">
                  <el-option label="Insert" value="INSERT" />
                  <el-option label="Upsert" value="UPSERT" />
                  <el-option label="Replace" value="REPLACE" />
                </el-select>
              </el-form-item>
            </template>
            <el-divider />
            <el-button type="danger" size="small" @click="deleteSelectedNode">{{ t('common.delete') }}</el-button>
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
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, VideoPlay, Close } from '@element-plus/icons-vue'
import NodePalette from '@/components/flow/NodePalette.vue'
import FlowCanvas from '@/components/flow/FlowCanvas.vue'
import { dataApi, type DataSourceTable } from '@/api/data'
import type { NodeSection } from '@/components/flow/NodePalette.vue'
import type { GraphNode } from '@vue-flow/core'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const pipelineName = ref('')
const pipelineDescription = ref('')
const saving = ref(false)
const running = ref(false)
const selectedNode = ref<GraphNode | null>(null)
const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const pipelineId = route.params.id as string | undefined

// Data source + real table catalogue for the DB Query node dropdowns.
const dataSources = ref<Array<{ id: number | string; name: string; type: string }>>([])
const sourceTables = ref<DataSourceTable[]>([])
const tablesLoading = ref(false)
const tableCache = new Map<string | number, DataSourceTable[]>()

let nodeCounter = 0

// The canvas registers node components by camelCase keys, but the backend
// persists the DAG with NodeType (EXTRACT/TRANSFORM/LOAD) + NodeSubType
// (DB_QUERY/FILTER/...). Map between the two so saved pipelines render and
// edits round-trip correctly.
const CANVAS_TO_SUBTYPE: Record<string, string> = {
  dbQuery: 'DB_QUERY',
  apiCall: 'API_CALL',
  fileRead: 'FILE_READ',
  filter: 'FILTER',
  map: 'MAP',
  join: 'JOIN',
  aggregate: 'AGGREGATE',
  script: 'SCRIPT',
  dbWrite: 'DB_WRITE',
  apiPush: 'API_PUSH',
}
const SUBTYPE_TO_CANVAS: Record<string, string> = Object.fromEntries(
  Object.entries(CANVAS_TO_SUBTYPE).map(([canvas, sub]) => [sub, canvas]),
)
const EXTRACT_TYPES = new Set(['dbQuery', 'apiCall', 'fileRead'])
const LOAD_TYPES = new Set(['dbWrite', 'apiPush', 'fileExport'])

function toBackendNodeCategory(canvasType: string): string {
  if (EXTRACT_TYPES.has(canvasType)) return 'EXTRACT'
  if (LOAD_TYPES.has(canvasType)) return 'LOAD'
  return 'TRANSFORM'
}

function toCanvasType(subType: string | undefined): string {
  if (!subType) return 'dbQuery'
  return SUBTYPE_TO_CANVAS[subType] ?? subType
}

const nodeSections: NodeSection[] = [
  {
    label: 'Extract',
    nodes: [
      { type: 'dbQuery', label: 'DB Query', icon: 'DB', color: '#409EFF' },
      { type: 'apiCall', label: 'API Call', icon: 'API', color: '#409EFF' },
      { type: 'fileRead', label: 'File Read', icon: 'FILE', color: '#409EFF' },
    ],
  },
  {
    label: 'Transform',
    nodes: [
      { type: 'filter', label: 'Filter', icon: 'F', color: '#E6A23C' },
      { type: 'map', label: 'Map', icon: 'M', color: '#E6A23C' },
      { type: 'join', label: 'Join', icon: 'J', color: '#E6A23C' },
      { type: 'aggregate', label: 'Aggregate', icon: 'A', color: '#E6A23C' },
    ],
  },
  {
    label: 'Load',
    nodes: [
      { type: 'dbWrite', label: 'DB Write', icon: 'W', color: '#67C23A' },
      { type: 'apiPush', label: 'API Push', icon: 'P', color: '#67C23A' },
    ],
  },
]

function onDrop(event: DragEvent) {
  const data = event.dataTransfer?.getData('application/vueflow')
  if (!data) return
  const nodeType = JSON.parse(data)
  const canvasEl = (event.target as HTMLElement).closest('.flow-canvas-wrapper')
  if (!canvasEl) return
  const rect = canvasEl.getBoundingClientRect()
  const x = event.clientX - rect.left - 70
  const y = event.clientY - rect.top - 20
  nodeCounter++
  nodes.value = [...nodes.value, {
    id: `node-${nodeCounter}`,
    type: nodeType.type,
    position: { x, y },
    data: { label: nodeType.label, config: {} },
  }]
}

function onNodeClick(node: GraphNode) { selectedNode.value = node }
function onPaneClick() { selectedNode.value = null }

function deleteSelectedNode() {
  if (!selectedNode.value) return
  const nodeId = selectedNode.value.id
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
  selectedNode.value = null
}

async function handleSave() {
  saving.value = true
  try {
    // Backend expects the DAG nested under `definition` with NodeType +
    // NodeSubType enums; map the canvas shape back to that contract.
    const payload = {
      name: pipelineName.value || t('pipeline.untitledPipeline'),
      description: pipelineDescription.value || undefined,
      definition: {
        nodes: nodes.value.map(n => ({
          id: n.id,
          type: toBackendNodeCategory(n.type),
          subType: CANVAS_TO_SUBTYPE[n.type] ?? n.type,
          name: n.data.label,
          config: n.data.config || {},
          position: n.position,
        })),
        edges: edges.value.map(e => ({ id: e.id, source: e.source, target: e.target })),
      },
    }
    if (pipelineId) {
      await dataApi.updatePipeline(pipelineId, payload)
    } else {
      const res = await dataApi.createPipeline(payload)
      const newId = (res as any).id
      if (newId) router.replace(`/data/pipelines/editor/${newId}`)
    }
    ElMessage.success(t('message.pipelineSaved'))
  } catch { /* handled */ } finally { saving.value = false }
}

async function handleRun() {
  running.value = true
  try {
    if (pipelineId) {
      await dataApi.executePipeline(pipelineId)
      ElMessage.success(t('message.pipelineExecutionStarted'))
    } else {
      ElMessage.warning(t('message.pleaseSavePipelineFirst'))
    }
  } catch { /* handled */ } finally { running.value = false }
}

async function loadDataSources() {
  try {
    const res: any = await dataApi.listSources({ page: 1, size: 200 })
    dataSources.value = (res?.records || []).map((s: any) => ({ id: s.id, name: s.name, type: s.type }))
  } catch {
    dataSources.value = []
  }
}

async function loadTables(sourceId: string | number) {
  if (tableCache.has(sourceId)) {
    sourceTables.value = tableCache.get(sourceId) as DataSourceTable[]
    return
  }
  tablesLoading.value = true
  try {
    const meta = await dataApi.getSourceMetadata(sourceId)
    const tables = meta?.tables || []
    tableCache.set(sourceId, tables)
    sourceTables.value = tables
  } catch {
    sourceTables.value = []
    ElMessage.warning(t('pipeline.loadTablesFailed'))
  } finally {
    tablesLoading.value = false
  }
}

function onSourceChange(sourceId: string | number) {
  if (!selectedNode.value) return
  // Switching the data source invalidates the previously chosen table.
  selectedNode.value.data.config.table = ''
  sourceTables.value = []
  if (sourceId) loadTables(sourceId)
}

function onTableChange(tableName: string) {
  if (!selectedNode.value || !tableName) return
  // Auto-fill a sensible default SQL if the user hasn't written one yet.
  const config = selectedNode.value.data.config
  if (!config.sql || /^select \* from /i.test(String(config.sql))) {
    config.sql = `SELECT * FROM ${tableName}`
  }
}

// When a DB Query node is selected, lazily load its source's table catalogue
// so the table dropdown is populated for the current selection.
watch(selectedNode, (node) => {
  if (node?.type === 'dbQuery') {
    const sourceId = node.data.config?.sourceId
    if (sourceId) {
      loadTables(sourceId)
    } else {
      sourceTables.value = []
    }
  }
})

onMounted(async () => {
  await loadDataSources()
  if (pipelineId) {
    try {
      const res = await dataApi.getPipeline(pipelineId)
      const pipeline = res as any
      pipelineName.value = pipeline.name || ''
      pipelineDescription.value = pipeline.description || ''
      // Nodes/edges live under `definition` in the backend VO; fall back to the
      // flat shape for older payloads.
      const rawNodes = pipeline.definition?.nodes || pipeline.nodes || []
      const rawEdges = pipeline.definition?.edges || pipeline.edges || []
      nodes.value = rawNodes.map((n: any) => ({
        id: n.id,
        type: toCanvasType(n.subType || n.type),
        position: n.position || { x: 0, y: 0 },
        data: { label: n.name || n.label || n.id, config: n.config || {} },
      }))
      edges.value = rawEdges.map((e: any, index: number) => ({
        id: e.id || `e-${e.source}-${e.target}-${index}`,
        source: e.source,
        target: e.target,
        type: 'smoothstep',
        animated: true,
      }))
      nodeCounter = nodes.value.length
    } catch { /* handled */ }
  }
})
</script>

<style scoped>
.pipeline-editor { display: flex; flex-direction: column; height: calc(100vh - 96px); margin: -20px; }
.pipeline-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; background: #fff; border-bottom: 1px solid var(--el-border-color-light); }
.toolbar-left { display: flex; gap: 12px; }
.toolbar-right { display: flex; gap: 8px; }
.pipeline-body { display: flex; flex: 1; overflow: hidden; }
.flow-canvas-wrapper { flex: 1; height: 100%; }
.properties-panel { width: 280px; background: #fff; border-left: 1px solid var(--el-border-color-light); overflow-y: auto; }
.properties-panel-empty { display: flex; align-items: center; justify-content: center; }
.panel-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; font-weight: 600; border-bottom: 1px solid var(--el-border-color-lighter); }
.panel-body { padding: 16px; }
.table-cols { float: right; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
