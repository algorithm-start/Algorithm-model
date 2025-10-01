<template>
  <div class="pipeline-list">
    <PageHeader :title="t('menu.pipelines')" :subtitle="t('form.manageDataPipelines')">
      <el-button type="primary" @click="router.push('/data/pipelines/editor')">
        <el-icon><Plus /></el-icon>
        {{ t('button.newPipeline') }}
      </el-button>
    </PageHeader>

    <el-card>
      <el-table :data="pipelines" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="name" :label="t('table.name')" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/data/pipelines/editor/${row.id}`)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('table.status')" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="t('form.description')" min-width="200" />
        <el-table-column prop="updatedAt" :label="t('table.updatedAt')" width="180" />
        <el-table-column :label="t('table.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/data/pipelines/editor/${row.id}`)">{{ t('common.edit') }}</el-button>
            <el-button link type="success" @click="handleExecute(row.id)">{{ t('common.run') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="pipelines.length === 0 && !loading" :description="t('common.noPipelines')" />
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
import { dataApi } from '@/api/data'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const router = useRouter()
const loading = ref(false)
const pipelines = ref<any[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await dataApi.listPipelines({})
    pipelines.value = (res as any).records || (res as any).items || (res as any).list || []
  } catch {
    pipelines.value = []
  } finally {
    loading.value = false
  }
}

async function handleExecute(id: string) {
  try {
    await dataApi.executePipeline(id)
    ElMessage.success(t('message.pipelineExecutionStarted'))
  } catch { /* handled */ }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm(t('message.deletePipelineConfirm'), t('message.confirmTitle'), { type: 'warning' })
    await dataApi.deletePipeline(id)
    ElMessage.success(t('message.pipelineDeleted'))
    loadData()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>
