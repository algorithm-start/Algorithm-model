<template>
  <div class="problem-detail">
    <PageHeader :title="problem?.problemName || t('form.problemDetail')" :subtitle="problem?.problemDescription">
      <el-button @click="router.back()">{{ t('common.back') }}</el-button>
      <el-button type="primary" plain @click="handleExport" :disabled="!problem">
        <el-icon><Download /></el-icon>
        导出报告
      </el-button>
      <el-button type="success" :loading="solving" @click="handleSolve" :disabled="problem?.status === 'RUNNING' || problem?.status === 'QUEUED'">
        {{ t('common.solve') }}
      </el-button>
    </PageHeader>

    <div v-loading="loading">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header><span>{{ t('form.problemInfo') }}</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item :label="t('table.name')">{{ problem?.problemName }}</el-descriptions-item>
              <el-descriptions-item :label="t('form.algorithm')">{{ problem?.algorithmType || '-' }}</el-descriptions-item>
              <el-descriptions-item :label="t('table.status')">
                <StatusTag v-if="problem" :status="problem.status" />
              </el-descriptions-item>
              <el-descriptions-item :label="t('table.createdTime')">{{ problem?.createTime }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="16">
          <el-card shadow="hover">
            <template #header><span>{{ t('form.solution') }}</span></template>
            <template v-if="problem?.status === 'OPTIMAL' || problem?.status === 'FEASIBLE'">
              <el-descriptions :column="2" border>
                <el-descriptions-item :label="t('form.objectiveValue')">
                  {{ problem?.objectiveValue?.toFixed(6) }}
                </el-descriptions-item>
                <el-descriptions-item :label="t('form.solveTime')">
                  {{ problem?.solveTimeFormatted || (problem?.solveTimeMs ? `${(problem.solveTimeMs / 1000).toFixed(2)}s` : '-') }}
                </el-descriptions-item>
              </el-descriptions>

              <h4 style="margin-top: 20px;">{{ t('form.variableValues') }}</h4>
              <el-table :data="variableResults" stripe border style="width: 100%">
                <el-table-column prop="name" :label="t('form.variable')" />
                <el-table-column prop="value" :label="t('form.value')">
                  <template #default="{ row }">
                    {{ row.value?.toFixed(6) }}
                  </template>
                </el-table-column>
              </el-table>

              <div ref="chartRef" style="height: 300px; margin-top: 20px;"></div>
            </template>
            <el-empty v-else-if="problem?.status === 'RUNNING' || problem?.status === 'QUEUED'" :description="t('form.solving')">
              <el-icon class="is-loading" :size="40"><Loading /></el-icon>
            </el-empty>
            <template v-else-if="problem?.status === 'ERROR' || problem?.status === 'INFEASIBLE'">
              <el-alert type="error" :closable="false" show-icon style="margin-bottom: 16px;">
                <template #title>
                  <span style="font-weight: 600;">求解失败</span>
                </template>
                <template #default>
                  <p style="margin: 4px 0;">{{ problem?.errorMessage || '未知错误' }}</p>
                </template>
              </el-alert>

              <el-collapse v-model="activeCollapse">
                <el-collapse-item title="💡 修改建议" name="suggestion">
                  <div class="suggestion-content">
                    <template v-if="problem?.errorMessage?.includes('Connection refused') || problem?.errorMessage?.includes('connect')">
                      <el-text type="warning">求解引擎服务未启动或连接失败。</el-text>
                      <ul>
                        <li>确认 solver-engine 服务已启动（默认端口 8001）</li>
                        <li>执行 <code>cd solver-engine && python -m uvicorn api.main:app --port 8001</code></li>
                        <li>确认 application.yml 中 solver.engine-url 配置正确</li>
                      </ul>
                    </template>
                    <template v-else-if="problem?.status === 'INFEASIBLE'">
                      <el-text type="warning">问题约束不可行，无法找到满足所有约束的解。</el-text>
                      <ul>
                        <li>检查约束条件是否存在矛盾</li>
                        <li>尝试放宽某些约束的边界值</li>
                        <li>确认变量范围定义是否合理</li>
                      </ul>
                    </template>
                    <template v-else>
                      <el-text type="warning">求解过程出现异常。</el-text>
                      <ul>
                        <li>检查问题定义格式是否正确</li>
                        <li>尝试选择其他求解算法重新执行</li>
                        <li>简化约束后重新测试</li>
                      </ul>
                    </template>
                  </div>
                </el-collapse-item>

                <el-collapse-item title="📋 问题定义" name="definition">
                  <el-table v-if="problemVariables.length" :data="problemVariables" stripe border size="small" style="margin-bottom: 12px;">
                    <el-table-column prop="name" label="变量名" width="140" />
                    <el-table-column prop="type" label="类型" width="100" />
                    <el-table-column prop="lowerBound" label="下界" width="80" />
                    <el-table-column prop="upperBound" label="上界" width="80" />
                  </el-table>
                  <p v-if="problemConstraints.length" style="font-weight: 500; margin: 8px 0;">约束条件 ({{ problemConstraints.length }})</p>
                  <el-table v-if="problemConstraints.length" :data="problemConstraints" stripe border size="small">
                    <el-table-column prop="expression" label="表达式" />
                    <el-table-column prop="sense" label="关系" width="60" />
                    <el-table-column prop="rhs" label="右值" width="80" />
                  </el-table>
                </el-collapse-item>

                <el-collapse-item title="🔍 日志跟踪" name="log">
                  <div class="log-content">
                    <p><strong>任务 ID：</strong>{{ problem?.id }}</p>
                    <p><strong>创建时间：</strong>{{ problem?.createTime }}</p>
                    <p><strong>更新时间：</strong>{{ problem?.updateTime }}</p>
                    <p><strong>算法类型：</strong>{{ problem?.algorithmType || '自动选择' }}</p>
                    <p><strong>状态：</strong>{{ problem?.statusDescription || problem?.status }}</p>
                    <p v-if="problem?.errorMessage"><strong>错误详情：</strong><code>{{ problem.errorMessage }}</code></p>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </template>
            <el-empty v-else :description="t('form.notSolvedYet')" />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { solverApi } from '@/api/solver'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const solving = ref(false)
const problem = ref<any>(null)
const variableResults = ref<any[]>([])
const chartRef = ref<HTMLElement>()
const activeCollapse = ref(['suggestion', 'definition', 'log'])

const parsedDefinition = computed(() => {
  if (!problem.value?.problemDefinition) return null
  try {
    return typeof problem.value.problemDefinition === 'string'
      ? JSON.parse(problem.value.problemDefinition)
      : problem.value.problemDefinition
  } catch {
    return null
  }
})

const problemVariables = computed(() => {
  const vars = parsedDefinition.value?.variables
  if (!Array.isArray(vars)) return []
  return vars.map((v: any) => ({
    name: v.name || v.variable_name || '-',
    type: v.type || v.variable_type || 'continuous',
    lowerBound: v.lowerBound ?? v.lower_bound ?? 0,
    upperBound: v.upperBound ?? v.upper_bound ?? '∞',
  }))
})

const problemConstraints = computed(() => {
  const cons = parsedDefinition.value?.constraints
  if (!Array.isArray(cons)) return []
  return cons.map((c: any) => ({
    expression: c.expression || c.lhs || '-',
    sense: c.sense || c.operator || '<=',
    rhs: c.rhs ?? c.right_hand_side ?? '-',
  }))
})

async function loadProblem() {
  loading.value = true
  try {
    const res = await solverApi.getProblem(route.params.id as string) as any
    problem.value = res
    // result is a JSON string from backend, parse it
    const parsedResult = typeof res.result === 'string' ? JSON.parse(res.result) : res.result
    if (parsedResult?.variables) {
      variableResults.value = Object.entries(parsedResult.variables).map(([name, value]) => ({
        name,
        value: value as number,
      }))
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleSolve() {
  solving.value = true
  try {
    const res = await solverApi.solveProblem(route.params.id as string) as any
    // Use returned data directly to update page state
    if (res) {
      problem.value = res
      const parsedResult = typeof res.result === 'string' ? JSON.parse(res.result) : res.result
      if (parsedResult?.variables) {
        variableResults.value = Object.entries(parsedResult.variables).map(([name, value]) => ({
          name,
          value: value as number,
        }))
      } else {
        variableResults.value = []
      }
    } else {
      await loadProblem()
    }
    if (problem.value?.status === 'OPTIMAL' || problem.value?.status === 'FEASIBLE') {
      ElMessage.success('求解完成')
    } else if (problem.value?.status === 'ERROR') {
      ElMessage.error(problem.value?.errorMessage || '求解失败')
    } else {
      ElMessage.info('求解已提交')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '求解请求失败')
    await loadProblem()
  } finally {
    solving.value = false
  }
}

function handleExport() {
  if (!problem.value) return

  const report: Record<string, any> = {
    exportTime: new Date().toISOString(),
    problemInfo: {
      id: problem.value.id,
      name: problem.value.problemName,
      description: problem.value.problemDescription,
      algorithmType: problem.value.algorithmType || '自动选择',
      status: problem.value.status,
      createTime: problem.value.createTime,
      updateTime: problem.value.updateTime,
    },
    problemDefinition: parsedDefinition.value || {},
    solveResult: {
      status: problem.value.status,
      objectiveValue: problem.value.objectiveValue,
      solveTimeMs: problem.value.solveTimeMs,
      solveTimeFormatted: problem.value.solveTimeFormatted,
      errorMessage: problem.value.errorMessage || null,
    },
  }

  // Add variable results if solved
  if (variableResults.value.length > 0) {
    report.solveResult.variables = Object.fromEntries(
      variableResults.value.map(v => [v.name, v.value])
    )
  }

  const jsonStr = JSON.stringify(report, null, 2)
  const blob = new Blob([jsonStr], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `solver-report-${problem.value.problemName || problem.value.id}-${Date.now()}.json`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('报告已导出')
}

function renderChart() {
  if (!chartRef.value || variableResults.value.length === 0) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: variableResults.value.map(v => v.name),
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: variableResults.value.map(v => v.value),
      itemStyle: { color: '#409EFF' },
    }],
  })
}

watch(variableResults, () => {
  nextTick(renderChart)
})

onMounted(loadProblem)
</script>

<style scoped>
.suggestion-content ul {
  margin: 8px 0;
  padding-left: 20px;
}
.suggestion-content li {
  margin: 4px 0;
  line-height: 1.8;
}
.suggestion-content code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}
.log-content p {
  margin: 6px 0;
  line-height: 1.6;
}
.log-content code {
  background: #fef0f0;
  color: #f56c6c;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  word-break: break-all;
}
</style>
