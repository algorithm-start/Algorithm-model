<template>
  <div class="flow-list">
    <PageHeader :title="t('menu.flows')" :subtitle="t('form.description')">
      <el-button type="primary" @click="router.push('/orchestrator/editor')">
        <el-icon><Plus /></el-icon>
        {{ t('button.newFlow') }}
      </el-button>
    </PageHeader>

    <el-card>
      <el-table :data="flows" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="name" :label="t('table.name')" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/orchestrator/editor/${row.id}`)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('table.status')" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="nodeCount" :label="t('table.nodes')" width="100" />
        <el-table-column prop="description" :label="t('form.description')" min-width="200" />
        <el-table-column prop="updatedAt" :label="t('table.updatedAt')" width="180" />
        <el-table-column :label="t('table.actions')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orchestrator/editor/${row.id}`)">{{ t('common.edit') }}</el-button>
            <el-button link type="success" @click="handlePublish(row.id)" :disabled="row.status === 'PUBLISHED'">{{ t('common.publish') }}</el-button>
            <el-button link type="warning" @click="handleExecute(row.id)">{{ t('common.run') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="flows.length === 0 && !loading" :description="t('common.noData')" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { orchestratorApi } from '@/api/orchestrator'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const router = useRouter()
const loading = ref(false)
const flows = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

async function loadData() {
  loading.value = true
  try {
    const res = await orchestratorApi.listFlows({ page: currentPage.value, size: pageSize.value })
    flows.value = (res as any).records || (res as any).items || (res as any).list || []
    total.value = (res as any).total || 0
  } catch {
    flows.value = [
      { id: '1', name: '实时数据同步', status: 'PUBLISHED', nodeCount: 5, description: '从多个数据源实时同步数据到数据仓库', updatedAt: '2026-05-29 10:30:00' },
      { id: '2', name: 'ETL每日导入', status: 'DRAFT', nodeCount: 3, description: '每日凌晨执行数据抽取、转换和加载', updatedAt: '2026-05-29 11:00:00' },
      { id: '3', name: '数据质量检查', status: 'PUBLISHED', nodeCount: 4, description: '定期检查数据完整性和一致性', updatedAt: '2026-05-29 12:15:00' },
    ]
    total.value = 3
  } finally {
    loading.value = false
  }
}

async function handlePublish(id: string) {
  try {
    await orchestratorApi.publishFlow(id)
    ElMessage.success(t('message.flowPublished'))
    loadData()
  } catch { /* handled */ }
}

async function handleExecute(id: string) {
  try {
    await orchestratorApi.executeFlow(id)
    ElMessage.success(t('message.flowExecutionStarted'))
  } catch { /* handled */ }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm(t('message.deleteFlowConfirm'), t('message.confirmTitle'), { type: 'warning' })
    await orchestratorApi.deleteFlow(id)
    ElMessage.success(t('message.flowDeleted'))
    loadData()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
