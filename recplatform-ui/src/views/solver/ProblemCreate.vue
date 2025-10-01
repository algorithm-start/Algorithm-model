<template>
  <div class="problem-create">
    <PageHeader :title="t('button.newProblem')" :subtitle="t('form.createNewProblem')">
      <el-dropdown @command="handleLoadExample" trigger="click" :loading="examplesLoading">
        <el-button type="primary">
          Load Example <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="ex in examples"
              :key="ex.id"
              :command="ex.id"
            >
              <span>{{ ex.name }}</span>
              <el-tag size="small" type="info" style="margin-left: 8px">{{ ex.type }}</el-tag>
            </el-dropdown-item>
            <el-dropdown-item v-if="examples.length === 0" disabled>
              No examples available
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button @click="router.back()">{{ t('common.cancel') }}</el-button>
    </PageHeader>

    <el-card>
      <el-steps :active="currentStep" finish-status="success" align-center class="steps">
        <el-step :title="t('form.basicInfo')" />
        <el-step :title="t('form.variables')" />
        <el-step :title="t('form.constraints')" />
        <el-step :title="t('form.objective')" />
        <el-step :title="t('form.algorithm')" />
      </el-steps>

      <div class="step-content">
        <!-- Step 1: Basic Info -->
        <div v-show="currentStep === 0">
          <el-form ref="basicFormRef" :model="form" :rules="basicRules" label-width="120px">
            <el-form-item :label="t('form.name')" prop="name">
              <el-input v-model="form.name" :placeholder="t('form.problemNamePlaceholder')" />
            </el-form-item>
            <el-form-item :label="t('form.description')" prop="description">
              <el-input v-model="form.description" type="textarea" :autosize="{ minRows: 3, maxRows: 20 }" :placeholder="t('form.problemDescPlaceholder')" />
            </el-form-item>
          </el-form>
        </div>

        <!-- Step 2: Variables -->
        <div v-show="currentStep === 1">
          <div class="section-header">
            <span>{{ t('form.decisionVariables') }}</span>
            <el-button type="primary" size="small" @click="addVariable">{{ t('button.addVariable') }}</el-button>
          </div>
          <el-table :data="form.variables" border style="width: 100%">
            <el-table-column :label="t('form.name')" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.name" :placeholder="t('form.variableName')" size="small" />
              </template>
            </el-table-column>
            <el-table-column :label="t('form.type')" width="160">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option :label="t('form.continuous')" value="CONTINUOUS" />
                  <el-option :label="t('form.integer')" value="INTEGER" />
                  <el-option :label="t('form.binary')" value="BINARY" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="t('form.lowerBound')" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.lowerBound" size="small" :step="1" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column :label="t('form.upperBound')" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.upperBound" size="small" :step="1" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column :label="t('table.actions')" width="80">
              <template #default="{ $index }">
                <el-button link type="danger" @click="form.variables.splice($index, 1)">{{ t('common.remove') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="form.variables.length === 0" :description="t('form.noVariables')" />
        </div>

        <!-- Step 3: Constraints -->
        <div v-show="currentStep === 2">
          <div class="section-header">
            <span>{{ t('form.constraints') }}</span>
            <el-button type="primary" size="small" @click="addConstraint">{{ t('button.addConstraint') }}</el-button>
          </div>
          <el-table :data="form.constraints" border style="width: 100%">
            <el-table-column :label="t('form.name')" width="160">
              <template #default="{ row }">
                <el-input v-model="row.name" :placeholder="t('form.constraintName')" size="small" />
              </template>
            </el-table-column>
            <el-table-column :label="t('form.expression')" min-width="200">
              <template #default="{ row }">
                <el-input v-model="row.expression" :placeholder="t('form.expressionPlaceholder')" size="small" />
              </template>
            </el-table-column>
            <el-table-column :label="t('form.type')" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option label="<=" value="LEQ" />
                  <el-option label="=" value="EQ" />
                  <el-option label=">=" value="GEQ" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="RHS" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.rhs" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column :label="t('table.actions')" width="80">
              <template #default="{ $index }">
                <el-button link type="danger" @click="form.constraints.splice($index, 1)">{{ t('common.remove') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="form.constraints.length === 0" :description="t('form.noConstraints')" />
        </div>

        <!-- Step 4: Objective -->
        <div v-show="currentStep === 3">
          <el-form label-width="120px">
            <el-form-item :label="t('form.direction')">
              <el-radio-group v-model="form.objective.sense">
                <el-radio value="MINIMIZE">{{ t('form.minimize') }}</el-radio>
                <el-radio value="MAXIMIZE">{{ t('form.maximize') }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item :label="t('form.expression')">
              <el-input v-model="form.objective.expression" :placeholder="t('form.objectiveExpression')" />
            </el-form-item>
          </el-form>
        </div>

        <!-- Step 5: Algorithm -->
        <div v-show="currentStep === 4">
          <el-form label-width="120px">
            <el-form-item :label="t('form.autoSelect')">
              <el-switch v-model="autoSelect" />
            </el-form-item>
            <el-form-item v-if="!autoSelect" :label="t('form.algorithm')">
              <el-select v-model="form.algorithmId" :placeholder="t('form.selectAlgorithm')" style="width: 100%">
                <el-option
                  v-for="algo in algorithms"
                  :key="algo.id"
                  :label="algo.name"
                  :value="algo.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="Start Solving">
              <el-switch v-model="startSolving" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="step-actions">
        <el-button v-if="currentStep > 0" @click="currentStep--">{{ t('common.previous') }}</el-button>
        <el-button v-if="currentStep < 4" type="primary" @click="handleNext">{{ t('common.next') }}</el-button>
        <el-button v-if="currentStep === 4" type="success" :loading="submitting" @click="handleSubmit">
          Create Problem
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { solverApi, examplesApi, type VariableDef, type ConstraintDef, type ObjectiveDef, type ExampleItem } from '@/api/solver'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const router = useRouter()

const currentStep = ref(0)
const submitting = ref(false)
const autoSelect = ref(true)
const startSolving = ref(false)
const basicFormRef = ref<FormInstance>()
const algorithms = ref<any[]>([])
const examples = ref<ExampleItem[]>([])
const examplesLoading = ref(false)

const form = reactive({
  name: '',
  description: '',
  variables: [] as VariableDef[],
  constraints: [] as ConstraintDef[],
  objective: {
    expression: '',
    sense: 'MINIMIZE' as 'MINIMIZE' | 'MAXIMIZE',
  } as ObjectiveDef,
  algorithmId: '',
  algorithmParams: {} as Record<string, unknown>,
})

const basicRules: FormRules = {
  name: [{ required: true, message: 'Please enter problem name', trigger: 'blur' }],
}

function addVariable() {
  form.variables.push({
    name: '',
    type: 'CONTINUOUS',
    lowerBound: 0,
    upperBound: 100,
  })
}

function addConstraint() {
  form.constraints.push({
    name: '',
    expression: '',
    type: 'LEQ',
    rhs: 0,
  })
}

async function handleNext() {
  if (currentStep.value === 0) {
    const valid = await basicFormRef.value?.validate().catch(() => false)
    if (!valid) return
  } else if (currentStep.value === 1) {
    if (form.variables.length === 0) {
      ElMessage.warning(t('message.atLeastOneVariable'))
      return
    }
    const emptyName = form.variables.find(v => !v.name?.trim())
    if (emptyName) {
      ElMessage.warning(t('message.variableNameRequired'))
      return
    }
  } else if (currentStep.value === 2) {
    if (form.constraints.length > 0) {
      const emptyExpr = form.constraints.find(c => !c.expression?.trim())
      if (emptyExpr) {
        ElMessage.warning(t('message.constraintExprRequired'))
        return
      }
    }
  } else if (currentStep.value === 3) {
    if (!form.objective.expression?.trim()) {
      ElMessage.warning(t('message.objectiveExprRequired'))
      return
    }
  }
  currentStep.value++
}

async function handleLoadExample(id: string) {
  try {
    const res = await examplesApi.getExample(id)
    const detail = res.data as any
    const problem = detail.problem || detail
    // Support both flat format (name/variables/constraints/objective)
    // and nested format (problemName/problemDefinition.variables/...)
    const def = problem.problemDefinition || problem
    const problemName = problem.problemName || problem.name || detail.name || ''
    const rawDesc = problem.description || detail.description || ''
    const problemDesc = rawDesc.replace(/\\n/g, '\n')

    form.name = problemName
    form.description = problemDesc
    form.variables = (def.variables || []).map((v: VariableDef) => ({
      name: v.name,
      type: v.type || 'CONTINUOUS',
      lowerBound: v.lowerBound ?? 0,
      upperBound: v.upperBound ?? 100,
    }))
    form.constraints = (def.constraints || []).map((c: ConstraintDef) => ({
      name: c.name,
      expression: c.expression,
      type: c.type || 'LEQ',
      rhs: c.rhs ?? 0,
    }))
    form.objective = {
      expression: def.objective?.expression || '',
      sense: def.objective?.sense || def.objective?.direction || 'MINIMIZE',
    }
    currentStep.value = 0
    ElMessage.success(t('message.exampleLoaded', { name: problemName }))
  } catch {
    ElMessage.error(t('message.loadExampleFailed'))
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await solverApi.createProblem({
      problemName: form.name,
      description: form.description || undefined,
      problemDefinition: {
        variables: form.variables,
        constraints: form.constraints,
        objective: {
          expression: form.objective.expression,
          sense: form.objective.sense
        },
        algorithmId: autoSelect.value ? undefined : form.algorithmId || undefined,
        algorithmParams: Object.keys(form.algorithmParams).length ? form.algorithmParams : undefined,
      }
    })
    const id = (res as any).id
    ElMessage.success(t('message.problemCreated'))
    if (startSolving.value && id) {
      await solverApi.solveProblem(id)
      ElMessage.success(t('message.problemSolvingStarted'))
    }
    router.push(`/solver/detail/${id}`)
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载算法列表
  try {
    const res = await solverApi.listAlgorithms()
    algorithms.value = (res as any).records || (res as any).items || (res as any).list || []
  } catch {
    algorithms.value = []
  }
  // 加载示例列表
  examplesLoading.value = true
  try {
    const res = await examplesApi.getExamples()
    examples.value = (res.data as ExampleItem[]) || []
  } catch {
    examples.value = []
  } finally {
    examplesLoading.value = false
  }
})
</script>

<style scoped>
.steps {
  margin-bottom: 30px;
}
.step-content {
  min-height: 300px;
  margin-bottom: 20px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
}
.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
