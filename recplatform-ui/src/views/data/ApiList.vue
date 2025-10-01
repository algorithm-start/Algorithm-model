<template>
  <div class="api-list">
    <PageHeader :title="t('form.dataApis')" :subtitle="t('form.publishedDataApis')" />

    <el-card>
      <el-table :data="apis" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="name" :label="t('table.name')" min-width="200" />
        <el-table-column prop="endpoint" :label="t('form.endpoint')" min-width="250">
          <template #default="{ row }">
            <el-link type="primary" :href="row.endpoint" target="_blank">{{ row.endpoint }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('table.status')" width="130">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="method" :label="t('form.method')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.method === 'GET' ? 'success' : 'warning'">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('table.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="handlePublish(row.id)" v-if="row.status !== 'PUBLISHED'">{{ t('common.publish') }}</el-button>
            <el-button link type="warning" @click="handleUnpublish(row.id)" v-else>{{ t('common.unpublish') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="apis.length === 0 && !loading" :description="t('form.noApisPublished')" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { dataApi } from '@/api/data'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const { t } = useI18n()
const apis = ref<any[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await dataApi.listApis({})
    apis.value = (res as any).records || (res as any).items || (res as any).list || []
  } catch { apis.value = [] } finally { loading.value = false }
}

async function handlePublish(id: string) {
  try {
    await dataApi.publishApi(id)
    ElMessage.success(t('message.apiPublished'))
    loadData()
  } catch { /* handled */ }
}

async function handleUnpublish(id: string) {
  try {
    await dataApi.unpublishApi(id)
    ElMessage.success(t('message.apiUnpublished'))
    loadData()
  } catch { /* handled */ }
}

onMounted(loadData)
</script>
