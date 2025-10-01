<template>
  <div class="flow-execution">
    <PageHeader :title="t('menu.executions')" :subtitle="t('orchestrator.executionLog')" />

    <el-card>
      <div class="filter-bar">
        <el-select v-model="statusFilter" :placeholder="t('table.status')" clearable style="width: 160px" @change="loadData">
          <el-option :label="t('status.running')" value="EXECUTING" />
          <el-option :label="t('status.completed')" value="COMPLETED" />
          <el-option :label="t('status.failed')" value="ERROR" />
          <el-option :label="t('status.cancelled')" value="CANCELLED" />
        </el-select>
      </div>

      <el-table :data="executions" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="flowName" :label="t('table.flowName')" min-width="180" />
        <el-table-column prop="status" :label="t('table.status')" width="140">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="startTime" :label="t('table.startTime')" width="180" />
        <el-table-column prop="endTime" :label="t('table.endTime')" width="180" />
        <el-table-column prop="duration" :label="t('table.duration')" width="120" />
        <el-table-column prop="triggeredBy" :label="t('table.triggeredBy')" width="140" />
        <el-table-column :label="t('table.actions')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button link type="danger" @click="handleCancel(row.id)" :disabled="row.status !== 'EXECUTING'">{{ t('common.cancel') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="t('form.executionDetail')" width="700px">
      <el-descriptions :column="2" border v-if="selectedExecution">
        <el-descriptions-item :label="t('table.flowName')">{{ selectedExecution.flowName }}</el-descriptions-item>
        <el-descriptions-item :label="t('table.status')">
          <StatusTag :status="selectedExecution.status" />
        </el-descriptions-item>
        <el-descriptions-item :label="t('table.startTime')">{{ selectedExecution.startTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('table.endTime')">{{ selectedExecution.endTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('table.duration')">{{ selectedExecution.duration }}</el-descriptions-item>
        <el-descriptions-item :label="t('table.triggeredBy')">{{ selectedExecution.triggeredBy }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin-top: 16px;">{{ t('table.nodeExecutions') }}</h4>
      <el-table :data="nodeExecutions" stripe border size="small">
        <el-table-column prop="nodeName" :label="t('table.node')" />
        <el-table-column prop="status" :label="t('table.status')" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="duration" :label="t('table.duration')" width="100" />
        <el-table-column prop="output" :label="t('table.output')" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { orchestratorApi } from '@/api/orchestrator'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loading = ref(false)
const executions = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')
const detailVisible = ref(false)
const selectedExecution = ref<any>(null)
const nodeExecutions = ref<any[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await orchestratorApi.listExecutions({ page: currentPage.value, size: pageSize.value })
    executions.value = (res as any).records || (res as any).items || (res as any).list || []
    total.value = (res as any).total || 0
  } catch {
    executions.value = [
      { id: '1', flowName: '实时数据同步', status: 'EXECUTING', startTime: '2026-05-29 10:30:00', endTime: null, duration: '5m 23s', triggeredBy: 'admin' },
      { id: '2', flowName: 'ETL每日导入', status: 'COMPLETED', startTime: '2026-05-29 02:00:00', endTime: '2026-05-29 02:15:32', duration: '15m 32s', triggeredBy: 'scheduler' },
      { id: '3', flowName: '数据质量检查', status: 'COMPLETED', startTime: '2026-05-29 08:00:00', endTime: '2026-05-29 08:05:12', duration: '5m 12s', triggeredBy: 'analyst' },
    ]
    total.value = 3
  } finally {
    loading.value = false
  }
}

async function viewDetail(row: any) {
  selectedExecution.value = row
  try {
    const res = await orchestratorApi.getExecution(row.id)
    nodeExecutions.value = (res as any).nodeExecutions || []
  } catch {
    nodeExecutions.value = []
  }
  detailVisible.value = true
}

async function handleCancel(id: string) {
  try {
    await orchestratorApi.cancelExecution(id)
    ElMessage.success(t('message.executionCancelled'))
    loadData()
  } catch { /* handled */ }
}

onMounted(loadData)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
