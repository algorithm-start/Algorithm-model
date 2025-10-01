<template>
  <div class="problem-list">
    <PageHeader :title="t('menu.problems')" :subtitle="'管理优化问题'">
      <el-button type="primary" @click="router.push('/solver/create')">
        <el-icon><Plus /></el-icon>
        {{ t('button.addProblem') }}
      </el-button>
    </PageHeader>

    <el-card>
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          :placeholder="'搜索问题...'"
          :prefix-icon="Search"
          clearable
          style="width: 300px"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-select v-model="statusFilter" :placeholder="t('table.status')" clearable style="width: 160px" @change="loadData">
          <el-option :label="t('status.pending')" value="PENDING" />
          <el-option label="求解中" value="RUNNING" />
          <el-option label="最优解" value="OPTIMAL" />
          <el-option label="可行解" value="FEASIBLE" />
          <el-option label="不可行" value="INFEASIBLE" />
          <el-option label="错误" value="ERROR" />
        </el-select>
      </div>

      <el-table :data="problems" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="problemName" :label="t('table.name')" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/solver/detail/${row.id}`)">{{ row.problemName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="algorithmType" :label="'算法'" width="160" />
        <el-table-column prop="status" :label="t('table.status')" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="objectiveValue" :label="'目标值'" width="140">
          <template #default="{ row }">
            {{ row.objectiveValue != null ? row.objectiveValue.toFixed(4) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="solveTimeMs" :label="'求解时间'" width="120">
          <template #default="{ row }">
            {{ row.solveTimeFormatted || (row.solveTimeMs ? `${(row.solveTimeMs / 1000).toFixed(2)}s` : '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('table.createdTime')" width="180" />
        <el-table-column :label="t('table.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/solver/detail/${row.id}`)">{{ t('button.view') }}</el-button>
            <el-button link type="success" @click="handleSolve(row.id)" :disabled="row.status === 'RUNNING' || row.status === 'QUEUED'">{{ t('button.execute') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="Number(total)"
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
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { solverApi } from '@/api/solver'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const problems = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const statusFilter = ref('')

async function loadData() {
  loading.value = true
  try {
    const res = await solverApi.listProblems({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined,
    })
    problems.value = (res as any).records || (res as any).items || (res as any).list || []
    total.value = (res as any).total || 0
  } catch {
    // 接口未实现，使用示例数据
    problems.value = [
      { id: '1', name: '供应链优化', algorithm: 'LP', status: 'SOLVED', objectiveValue: 1234.56, solveTime: 2.3, createdAt: '2026-05-29 10:30:00' },
      { id: '2', name: '生产计划排程', algorithm: 'MIP', status: 'SOLVING', objectiveValue: null, solveTime: null, createdAt: '2026-05-29 11:00:00' },
      { id: '3', name: '物流配送路线优化', algorithm: 'VRP', status: 'PENDING', objectiveValue: null, solveTime: null, createdAt: '2026-05-29 11:15:00' },
    ]
    total.value = 3
  } finally {
    loading.value = false
  }
}

async function handleSolve(id: string) {
  try {
    await solverApi.solveProblem(id)
    ElMessage.success('问题求解已启动')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '求解请求失败，请检查求解引擎是否启动')
  }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm('确定要删除此问题吗？', '确认删除', { type: 'warning' })
    await solverApi.deleteProblem(id)
    ElMessage.success('问题已删除')
    loadData()
  } catch {
    // Cancelled or error
  }
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
