<template>
  <div class="system-info" v-loading="loading">
    <PageHeader :title="t('system.title')" :subtitle="t('system.subtitle')">
      <el-button :icon="Refresh" @click="loadSystemInfo">{{ t('common.refresh') }}</el-button>
    </PageHeader>

    <el-alert
      v-if="loadError"
      :title="t('system.loadFailed')"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 16px;"
    />

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>{{ t('system.serviceHealth') }}</span></template>
          <div class="health-list">
            <div v-for="service in services" :key="service.name" class="health-item">
              <div class="health-left">
                <span class="service-name">{{ service.name }}</span>
                <span class="service-version">{{ service.version }}</span>
              </div>
              <div class="health-right">
                <el-tag :type="service.healthy ? 'success' : 'danger'" size="small">
                  {{ service.healthy ? t('system.healthy') : t('system.down') }}
                </el-tag>
                <span class="service-uptime">{{ service.uptime }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>{{ t('system.systemMetrics') }}</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="t('system.cpuUsage')">
              <el-progress :percentage="metrics.cpu" :color="getProgressColor(metrics.cpu)" />
            </el-descriptions-item>
            <el-descriptions-item :label="t('system.memoryUsage')">
              <el-progress :percentage="metrics.memory" :color="getProgressColor(metrics.memory)" />
            </el-descriptions-item>
            <el-descriptions-item :label="t('system.diskUsage')">
              <el-progress :percentage="metrics.disk" :color="getProgressColor(metrics.disk)" />
            </el-descriptions-item>
            <el-descriptions-item :label="t('system.activeConnections')">
              {{ metrics.activeConnections }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header><span>{{ t('system.jvmInfo') }}</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item :label="t('system.javaVersion')">{{ jvmInfo.javaVersion }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.heapUsed')">{{ jvmInfo.heapUsed }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.heapMax')">{{ jvmInfo.heapMax }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.gcCount')">{{ jvmInfo.gcCount }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.gcTime')">{{ jvmInfo.gcTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('system.threads')">{{ jvmInfo.threads }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import request from '@/api/index'

const { t } = useI18n()

const loading = ref(false)
const loadError = ref(false)
const services = ref<any[]>([])
const metrics = ref({
  cpu: 0,
  memory: 0,
  disk: 0,
  activeConnections: 0,
})
const jvmInfo = ref({
  javaVersion: '-',
  heapUsed: '-',
  heapMax: '-',
  gcCount: 0,
  gcTime: '-',
  threads: 0,
})

function getProgressColor(percentage: number): string {
  if (percentage < 60) return '#67C23A'
  if (percentage < 80) return '#E6A23C'
  return '#F56C6C'
}

async function loadSystemInfo() {
  loading.value = true
  loadError.value = false
  try {
    const data = (await request.get('/admin/system/info')) as any
    services.value = data.services ?? []
    metrics.value = data.metrics ?? { cpu: 0, memory: 0, disk: 0, activeConnections: 0 }
    jvmInfo.value = data.jvmInfo ?? jvmInfo.value
  } catch {
    loadError.value = true
    services.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadSystemInfo)
</script>

<style scoped>
.health-list { display: flex; flex-direction: column; gap: 12px; }
.health-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.health-item:last-child { border-bottom: none; }
.health-left { display: flex; flex-direction: column; }
.service-name { font-weight: 600; font-size: 14px; }
.service-version { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
.health-right { display: flex; align-items: center; gap: 12px; }
.service-uptime { font-size: 13px; color: var(--el-text-color-secondary); }
</style>
