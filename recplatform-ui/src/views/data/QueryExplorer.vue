<template>
  <div class="query-explorer">
    <PageHeader :title="t('menu.query')" :subtitle="t('form.adHocQuery')" />

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span>{{ t('form.query') }}</span></template>
          <el-form label-position="top">
            <el-form-item :label="t('form.dataSource')">
              <el-select v-model="queryForm.sourceId" :placeholder="t('form.selectDataSource')"
                style="width: 100%" @change="onSourceChange">
                <el-option
                  v-for="src in sources"
                  :key="src.id"
                  :label="src.name"
                  :value="src.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <template #label>
                <span>SQL</span>
                <span v-if="schemaLoading" class="schema-hint">{{ t('query.loadingSchema') }}</span>
                <span v-else-if="tableCount" class="schema-hint">
                  {{ t('query.schemaReady', { count: tableCount }) }}
                </span>
              </template>
              <SqlEditor v-model="queryForm.sql" :schema="sqlSchema" min-height="280px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="executing" @click="handleExecute">
                <el-icon><VideoPlay /></el-icon> {{ t('common.execute') }}
              </el-button>
              <el-button @click="handleExport" :disabled="!resultColumns.length">
                <el-icon><Download /></el-icon>{{ t('common.export') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span>{{ t('form.results') }}</span>
            <span v-if="resultData.length" style="margin-left: 12px; color: var(--el-text-color-secondary); font-size: 13px;">
              {{ resultData.length }} {{ t('form.rows') }}
            </span>
          </template>
          <el-table :data="resultData" stripe border height="500" v-if="resultColumns.length" style="width: 100%">
            <el-table-column
              v-for="col in resultColumns"
              :key="col"
              :prop="col"
              :label="col"
              min-width="120"
            />
          </el-table>
          <el-empty v-else :description="t('message.executeQueryToSeeResults')" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import SqlEditor, { type SqlSchema } from '@/components/common/SqlEditor.vue'
import { dataApi } from '@/api/data'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const executing = ref(false)
const sources = ref<any[]>([])
const resultColumns = ref<string[]>([])
const resultData = ref<any[]>([])

// Table/column catalogue feeding the SQL editor's autocompletion. Refreshed
// whenever the selected data source changes; the editor offers table names and
// — after a dot — that table's columns.
const sqlSchema = ref<SqlSchema>({})
const schemaLoading = ref(false)
const schemaCache = new Map<string | number, SqlSchema>()
const tableCount = computed(() => Object.keys(sqlSchema.value).length)

const queryForm = reactive({
  sourceId: '',
  sql: '',
})

async function loadSources() {
  try {
    const res = await dataApi.listSources({}) as any
    sources.value = res.records || res.items || res.list || []
  } catch { sources.value = [] }
}

async function onSourceChange(sourceId: string | number) {
  sqlSchema.value = {}
  if (!sourceId) return
  if (schemaCache.has(sourceId)) {
    sqlSchema.value = schemaCache.get(sourceId) as SqlSchema
    return
  }
  schemaLoading.value = true
  try {
    const meta = await dataApi.getSourceMetadata(sourceId)
    const schema: SqlSchema = {}
    for (const table of meta?.tables || []) {
      schema[table.name] = (table.columns || []).map((col) => col.name)
    }
    schemaCache.set(sourceId, schema)
    sqlSchema.value = schema
  } catch {
    // The source may be unreachable (e.g. demo placeholders); keep keyword-only
    // completion and let the user query manually.
    sqlSchema.value = {}
  } finally {
    schemaLoading.value = false
  }
}

async function handleExecute() {
  if (!queryForm.sourceId) {
    ElMessage.warning(t('message.pleaseSelectDataSource'))
    return
  }
  if (!queryForm.sql.trim()) {
    ElMessage.warning(t('message.pleaseEnterSQL'))
    return
  }
  executing.value = true
  try {
    const res = await dataApi.executeQuery({ dataSourceId: queryForm.sourceId, query: queryForm.sql })
    const data = (res as any).rows || []
    if (data.length > 0) {
      resultColumns.value = Object.keys(data[0])
      resultData.value = data
    } else {
      resultColumns.value = []
      resultData.value = []
      ElMessage.info(t('message.queryNoResults'))
    }
  } catch {
    resultColumns.value = []
    resultData.value = []
  } finally {
    executing.value = false
  }
}

function handleExport() {
  if (!resultData.value.length) return
  const headers = resultColumns.value.join(',')
  const rows = resultData.value.map(row => resultColumns.value.map(col => row[col]).join(','))
  const csv = [headers, ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'query_result.csv'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadSources)
</script>

<style scoped>
.schema-hint {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}
</style>
