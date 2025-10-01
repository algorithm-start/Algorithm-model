<template>
  <div class="audit-log">
    <PageHeader :title="t('menu.audit')" :subtitle="t('admin.auditLogSubtitle')">
      <el-button @click="handleExport" :loading="exporting">
        <el-icon><Download /></el-icon>{{ t('common.export') }}</el-button>
    </PageHeader>

    <el-card>
      <div class="filter-bar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          :start-placeholder="'开始日期'"
          :end-placeholder="'结束日期'"
          style="width: 280px"
          @change="loadData"
        />
        <el-input v-model="filters.username" :placeholder="'用户'" clearable style="width: 140px" @clear="loadData" @keyup.enter="loadData" />
        <el-select v-model="filters.action" :placeholder="'操作'" clearable style="width: 160px" @change="loadData">
          <el-option label="创建" value="CREATE" />
          <el-option label="更新" value="UPDATE" />
          <el-option :label="t('common.delete')" value="DELETE" />
          <el-option label="执行" value="EXECUTE" />
          <el-option label="登录" value="LOGIN" />
        </el-select>
        <el-select v-model="filters.result" :placeholder="'结果'" clearable style="width: 140px" @change="loadData">
          <el-option :label="t('status.success')" value="SUCCESS" />
          <el-option label="失败" value="FAILURE" />
        </el-select>
      </div>

      <el-table :data="logs" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="timestamp" :label="t('dashboard.time')" width="180" />
        <el-table-column prop="username" :label="t('dashboard.user')" width="120" />
        <el-table-column prop="action" :label="t('dashboard.action')" width="120" />
        <el-table-column prop="resource" :label="t('dashboard.resource')" min-width="200" />
        <el-table-column prop="result" :label="t('dashboard.status')" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.result" />
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="duration" :label="'耗时'" width="100">
          <template #default="{ row }">
            {{ row.duration ? `${row.duration}ms` : '-' }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100]"
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
import { ref, reactive, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { auditApi } from '@/api/audit'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const { t } = useI18n()
const exporting = ref(false)
const logs = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dateRange = ref<[Date, Date] | null>(null)

const filters = reactive({
  username: '',
  action: '',
  result: '',
})

async function loadData() {
  loading.value = true
  try {
    const res = await auditApi.listLogs({
      page: currentPage.value,
      size: pageSize.value,
      username: filters.username || undefined,
      action: filters.action || undefined,
      result: filters.result || undefined,
      startTime: dateRange.value?.[0]?.toISOString(),
      endTime: dateRange.value?.[1]?.toISOString(),
    })
    const records = (res as any).records || (res as any).items || (res as any).list || []
    // Map backend VO fields to frontend display fields
    logs.value = records.map((log: any) => ({
      ...log,
      timestamp: log.createTime ? log.createTime.replace('T', ' ').substring(0, 19) : '',
    }))
    total.value = (res as any).total || 0
  } catch {
    logs.value = []
    total.value = 0
  } finally { loading.value = false }
}

async function handleExport() {
  exporting.value = true
  try {
    await auditApi.exportLogs({
      username: filters.username || undefined,
      action: filters.action || undefined,
      result: filters.result || undefined,
      startTime: dateRange.value?.[0]?.toISOString(),
      endTime: dateRange.value?.[1]?.toISOString(),
    })
  } catch { /* handled */ } finally { exporting.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
