<template>
  <div class="dashboard">
    <PageHeader :title="t('dashboard.title')" :subtitle="t('dashboard.subtitle')" />

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">{{ t('dashboard.totalProblems') }}</div>
              <div class="stat-value">{{ stats.totalProblems }}</div>
            </div>
            <el-icon class="stat-icon" style="color: #1677ff"><Cpu /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">{{ t('dashboard.activePipelines') }}</div>
              <div class="stat-value">{{ stats.activePipelines }}</div>
            </div>
            <el-icon class="stat-icon" style="color: #67C23A"><DataAnalysis /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">{{ t('dashboard.runningFlows') }}</div>
              <div class="stat-value">{{ stats.runningFlows }}</div>
            </div>
            <el-icon class="stat-icon" style="color: #E6A23C"><Connection /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">{{ t('dashboard.users') }}</div>
              <div class="stat-value">{{ stats.users }}</div>
            </div>
            <el-icon class="stat-icon" style="color: #909399"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>{{ t('dashboard.recentActivity') }}</span>
          </template>
          <el-table :data="recentActivity" stripe style="width: 100%">
            <el-table-column prop="time" :label="t('dashboard.time')" width="180" />
            <el-table-column prop="user" :label="t('dashboard.user')" width="120" />
            <el-table-column prop="action" :label="t('dashboard.action')" />
            <el-table-column prop="resource" :label="t('dashboard.resource')" />
            <el-table-column prop="status" :label="t('dashboard.status')" width="100">
              <template #default="{ row }">
                <StatusTag :status="row.status" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>{{ t('dashboard.systemHealth') }}</span>
          </template>
          <div class="health-list">
            <div v-for="item in healthItems" :key="item.name" class="health-item">
              <span class="health-name">{{ item.name }}</span>
              <el-tag :type="item.healthy ? 'success' : 'danger'" size="small">
                {{ item.healthy ? t('dashboard.healthy') : t('dashboard.down') }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { Cpu, DataAnalysis, Connection, User } from '@element-plus/icons-vue'

const { t } = useI18n()

const stats = ref({
  totalProblems: 24,
  activePipelines: 8,
  runningFlows: 3,
  users: 12,
})

const recentActivity = ref([
  { time: '2025-01-15 10:30', user: 'admin', action: t('dashboard.actions.createdProblem'), resource: 'Supply Chain Optimization', status: 'SUCCESS' },
  { time: '2025-01-15 10:15', user: 'analyst', action: t('dashboard.actions.executedPipeline'), resource: 'ETL Daily Import', status: 'RUNNING' },
  { time: '2025-01-15 09:45', user: 'admin', action: t('dashboard.actions.publishedFlow'), resource: 'Data Validation Flow', status: 'SUCCESS' },
  { time: '2025-01-15 09:30', user: 'engineer', action: t('dashboard.actions.solvedProblem'), resource: 'Route Optimization', status: 'COMPLETED' },
  { time: '2025-01-15 09:00', user: 'admin', action: t('dashboard.actions.addedDataSource'), resource: 'Production DB', status: 'SUCCESS' },
])

const healthItems = computed(() => [
  { name: t('dashboard.services.apiServer'), healthy: true },
  { name: t('dashboard.services.solverEngine'), healthy: true },
  { name: t('dashboard.services.orchestrator'), healthy: true },
  { name: t('dashboard.services.database'), healthy: true },
  { name: t('dashboard.services.messageQueue'), healthy: false },
])
</script>

<style scoped>
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  border-radius: 8px;
}
.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.stat-icon {
  font-size: 40px;
  opacity: 0.8;
}
.health-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.health-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.health-item:last-child {
  border-bottom: none;
}
.health-name {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
</style>
