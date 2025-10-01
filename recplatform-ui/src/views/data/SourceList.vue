<template>
  <div class="source-list">
    <PageHeader :title="t('menu.sources')" :subtitle="t('form.manageDataConnections')">
      <el-button type="primary" @click="router.push('/data/sources/create')">
        <el-icon><Plus /></el-icon>
        {{ t('button.addSource') }}
      </el-button>
    </PageHeader>

    <el-card>
      <el-table :data="sources" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="name" :label="t('table.name')" min-width="180" />
        <el-table-column prop="type" :label="t('table.type')" width="140">
          <template #default="{ row }">
            <el-tag :type="getTypeTagColor(row.type)" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('table.status')" width="130">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="lastTestAt" :label="t('data.lastTest')" width="180" />
        <el-table-column prop="createdAt" :label="t('table.createdTime')" width="180" />
        <el-table-column :label="t('table.actions')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="handleTest(row.id)" :loading="row.testing">{{ t('common.testConnection') }}</el-button>
            <el-button link type="primary" @click="router.push(`/data/sources/create?id=${row.id}`)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="sources.length === 0 && !loading" :description="t('common.noDataSources')" />
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
const sources = ref<any[]>([])

function getTypeTagColor(type: string): string {
  const map: Record<string, string> = {
    MYSQL: '', POSTGRESQL: '', ORACLE: 'warning', SQLSERVER: 'success',
    REDIS: 'danger', MONGODB: 'success', ELASTICSEARCH: 'warning',
    KAFKA: 'info', API: '', FILE: 'info',
  }
  return map[type] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await dataApi.listSources({}) as any
    sources.value = res.records || res.items || res.list || []
  } catch {
    sources.value = []
  } finally {
    loading.value = false
  }
}

async function handleTest(id: string) {
  try {
    const result = await dataApi.testConnection(id) as any
    if (result?.success) {
      ElMessage.success(result.message || t('message.connectionTestSuccessful'))
    } else {
      ElMessage.error(result?.message || t('message.connectionTestFailed'))
    }
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('message.connectionTestFailed'))
  }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm(t('message.deleteSourceConfirm'), t('message.confirmTitle'), { type: 'warning' })
    await dataApi.deleteSource(id)
    ElMessage.success(t('message.sourceDeleted'))
    loadData()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>
