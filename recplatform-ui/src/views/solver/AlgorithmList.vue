<template>
  <div class="algorithm-list">
    <PageHeader :title="t('menu.algorithms')" :subtitle="t('form.browseAlgorithms')">
      <el-button v-if="canEditAlgorithm" type="primary" @click="openRegisterDialog">
        <el-icon><Plus /></el-icon>
        注册自定义算法
      </el-button>
      <el-tag v-else type="info" effect="plain">只读模式 · 无法修改算法配置</el-tag>
    </PageHeader>

    <!-- 能力总览 -->
    <el-card shadow="never" class="capability-card">
      <div class="capability-grid">
        <div class="capability-item" v-for="cap in solverCapabilities" :key="cap.label">
          <el-tag :type="cap.ready ? 'success' : 'info'" effect="dark" size="large">{{ cap.label }}</el-tag>
          <span class="cap-desc">{{ cap.desc }}</span>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="8" v-for="algo in allAlgorithms" :key="algo.code">
        <el-card shadow="hover" class="algo-card">
          <template #header>
            <div class="algo-header">
              <span class="algo-name">{{ algo.name }}</span>
              <el-tag
                :type="algo.category === '精确求解' ? 'success' : 'warning'"
                size="small"
              >
                {{ algo.category }}
              </el-tag>
            </div>
          </template>
          <p class="algo-desc">{{ algo.description }}</p>
          <div class="algo-meta">
            <el-tag
              v-for="pt in algo.problemTypes"
              :key="pt"
              size="small"
              type="info"
              class="algo-type-tag"
            >
              {{ pt }}
            </el-tag>
          </div>
          <div class="algo-params" v-if="algo.params">
            <el-divider content-position="left">{{ t('form.keyParameters') }}</el-divider>
            <div v-for="(desc, key) in algo.params" :key="key" class="algo-param-item">
              <code class="param-key">{{ key }}</code>
              <span class="param-desc">{{ desc }}</span>
            </div>
          </div>
          <div class="algo-code">
            <span class="code-label">Code:</span>
            <code>{{ algo.code }}</code>
          </div>
          <div class="algo-actions">
            <el-button v-if="canEditAlgorithm" type="primary" size="small" @click="openConfigDialog(algo)">
              <el-icon><Setting /></el-icon>
              {{ t('common.configure') }}
            </el-button>
            <el-button size="small" @click="showHelp(algo)">
              <el-icon><QuestionFilled /></el-icon>
              {{ t('common.help') }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 配置对话框 -->
    <el-dialog v-model="configDialogVisible" :title="t('form.configureAlgorithm')" width="600px">
      <el-form :model="configForm" label-width="150px" v-if="selectedAlgorithm">
        <el-form-item :label="t('form.algorithmName')">
          <el-input :value="selectedAlgorithm.name" disabled />
        </el-form-item>
        <el-divider content-position="left">{{ t('form.parameterSettings') }}</el-divider>
        <el-form-item v-for="(desc, key) in selectedAlgorithm.params" :key="key" :label="key">
          <el-input v-model="configForm[key as string]" :placeholder="desc" />
          <el-tooltip :content="desc" placement="top">
            <el-icon class="help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveConfig">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 帮助对话框 -->
    <el-dialog v-model="helpDialogVisible" :title="t('form.algorithmHelp')" :width="selectedAlgorithm?.helpSections ? '900px' : '700px'" top="5vh">
      <div v-if="selectedAlgorithm" class="help-dialog-body">
        <h3>{{ selectedAlgorithm.name }}</h3>
        <p style="line-height: 1.8; color: var(--el-text-color-primary);">{{ selectedAlgorithm.description }}</p>

        <!-- 有详细帮助内容时展示 -->
        <template v-if="selectedAlgorithm.helpSections">
          <!-- 核心区别速览表（OR-Tools 专用） -->
          <div v-if="selectedAlgorithm.code === 'ORTOOLS'" class="compare-table-wrapper">
            <h4 style="margin-top: 20px;">核心区别速览</h4>
            <el-table :data="ortoolsCompareData" border stripe style="width: 100%; margin-top: 12px;" size="small">
              <el-table-column prop="feature" label="特性" width="140" fixed />
              <el-table-column prop="glop" label="GLOP" min-width="180" />
              <el-table-column prop="scip" label="SCIP" min-width="180" />
              <el-table-column prop="cpsat" label="CP-SAT" min-width="200" />
            </el-table>
          </div>

          <div v-for="(section, idx) in selectedAlgorithm.helpSections" :key="idx" class="help-section">
            <h4>{{ section.title }}</h4>
            <pre class="help-content">{{ section.content }}</pre>
          </div>
        </template>

        <!-- 无详细帮助时展示默认内容 -->
        <template v-else>
          <h4 style="margin-top: 20px;">{{ t('form.applicableProblemTypes') }}</h4>
          <div class="help-tags">
            <el-tag v-for="pt in selectedAlgorithm.problemTypes" :key="pt" type="info" size="large">
              {{ pt }}
            </el-tag>
          </div>

          <h4 style="margin-top: 20px;">{{ t('form.parameterDescription') }}</h4>
          <el-descriptions :column="1" border v-if="selectedAlgorithm.params">
            <el-descriptions-item v-for="(desc, key) in selectedAlgorithm.params" :key="key" :label="key">
              {{ desc }}
            </el-descriptions-item>
          </el-descriptions>

          <h4 style="margin-top: 20px;">{{ t('form.usageGuidelines') }}</h4>
          <ul style="line-height: 2; padding-left: 20px;">
            <li v-if="selectedAlgorithm.category === '精确求解'">
              {{ t('form.exactAlgorithmGuide') }}
            </li>
            <li v-else>
              {{ t('form.heuristicAlgorithmGuide') }}
            </li>
            <li>{{ t('form.parameterTuningTip') }}</li>
            <li>{{ t('form.performanceNote') }}</li>
          </ul>
        </template>
      </div>
    </el-dialog>

    <!-- 自定义算法注册对话框 -->
    <el-dialog v-model="registerDialogVisible" title="注册自定义算法" width="720px" top="5vh">
      <el-form :model="registerForm" label-width="120px">
        <el-form-item label="算法名称" required>
          <el-input v-model="registerForm.name" placeholder="例如：Gurobi 求解器" />
        </el-form-item>
        <el-form-item label="算法编码" required>
          <el-input v-model="registerForm.code" placeholder="唯一标识，如 GUROBI" />
        </el-form-item>
        <el-form-item label="算法描述">
          <el-input v-model="registerForm.description" type="textarea" :rows="2" placeholder="描述算法用途和特点" />
        </el-form-item>
        <el-form-item label="适用问题类型" required>
          <el-checkbox-group v-model="registerForm.problemTypes">
            <el-checkbox v-for="pt in problemTypeOptions" :key="pt" :label="pt" :value="pt">{{ pt }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-divider content-position="left">接入方式</el-divider>
        <el-form-item label="集成类型">
          <el-radio-group v-model="registerForm.integrationType">
            <el-radio value="api">外部 API 端点</el-radio>
            <el-radio value="script">Python 脚本</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="registerForm.integrationType === 'api'" label="API 端点" required>
          <el-input v-model="registerForm.apiEndpoint" placeholder="http://your-solver:8080/solve">
            <template #prepend>POST</template>
          </el-input>
          <div class="form-tip">API 需接受 JSON 请求体 {"problem": {...}, "config": {...}}，返回 {"status": "optimal", "variables": {...}, "objective_value": 0.0}</div>
        </el-form-item>

        <el-form-item v-if="registerForm.integrationType === 'script'" label="Python 脚本" required>
          <div class="script-input-wrapper">
            <el-upload
              :show-file-list="false"
              :before-upload="handleScriptUpload"
              accept=".py"
              class="script-upload"
            >
              <el-button size="small">
                <el-icon><Upload /></el-icon>
                上传 .py 文件
              </el-button>
            </el-upload>
            <span v-if="uploadedFileName" class="uploaded-file">已上传：{{ uploadedFileName }}</span>
          </div>
          <el-input v-model="registerForm.scriptContent" type="textarea" :rows="8" placeholder="from solvers.base import BaseSolver&#10;&#10;class CustomSolver(BaseSolver):&#10;    def solve(self, problem):&#10;        # 实现求解逻辑&#10;        pass" />
          <div class="form-tip">脚本需继承 BaseSolver 并实现 solve 方法，或上传 .py 文件自动填充内容</div>
        </el-form-item>

        <el-divider content-position="left">参数定义（可选）</el-divider>
        <div v-for="(param, index) in registerForm.params" :key="index" class="param-row">
          <el-input v-model="param.key" placeholder="参数名" style="width: 180px" />
          <el-input v-model="param.desc" placeholder="参数说明" style="flex: 1" />
          <el-button type="danger" link @click="removeParam(index)">删除</el-button>
        </div>
        <el-button type="primary" link @click="addParam" style="margin-top: 8px;">+ 添加参数</el-button>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomAlgorithm">注册算法</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, QuestionFilled, Plus, Upload } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { algorithmApi } from '@/api/solver'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()
// VIEWER (read-only) users may browse algorithms but cannot edit configs
// or register custom algorithms.
const canEditAlgorithm = computed(() => userStore.canEditAlgorithm)

interface AlgorithmInfo {
  name: string
  code: string
  category: string
  problemTypes: string[]
  description: string
  params?: Record<string, string>
  helpSections?: { title: string; content: string }[]
}

const algorithms: AlgorithmInfo[] = [
  {
    name: '单纯形法',
    code: 'SIMPLEX',
    category: '精确求解',
    problemTypes: ['LP'],
    description: '经典线性规划算法，两阶段法处理等式约束与人工变量，适合中小规模线性规划问题。',
    params: {
      max_iterations: '最大迭代次数',
      tolerance: '最优性容差',
    },
  },
  {
    name: '分支定界法',
    code: 'BRANCH_BOUND',
    category: '精确求解',
    problemTypes: ['MILP'],
    description: '整数规划标准算法，通过分支、定界与剪枝系统搜索整数最优解。',
    params: {
      max_nodes: '最大搜索节点数',
      mip_gap: 'MIP 间隙容差',
    },
  },
  {
    name: '内点法',
    code: 'INTERIOR_POINT',
    category: '精确求解',
    problemTypes: ['LP', 'QP'],
    description: '原始-对偶内点法，多项式时间复杂度，适合大规模线性与二次规划问题。',
    params: {
      max_iterations: '最大迭代次数',
      barrier_tol: '障碍函数容差',
    },
  },
  {
    name: '遗传算法',
    code: 'GA',
    category: '启发式',
    problemTypes: ['LP', 'MILP', 'NLP', 'MINLP'],
    description: '基于进化的全局优化算法，通过选择、交叉、变异搜索近似最优解，适合复杂非凸问题。',
    params: {
      population_size: '种群大小',
      generations: '进化代数',
      crossover_rate: '交叉概率',
      mutation_rate: '变异概率',
    },
  },
  {
    name: '模拟退火',
    code: 'SA',
    category: '启发式',
    problemTypes: ['LP', 'MILP', 'NLP', 'MINLP'],
    description: '概率搜索算法，通过模拟物理退火过程避免陷入局部最优，适合复杂搜索空间。',
    params: {
      initial_temp: '初始温度',
      cooling_rate: '冷却速率',
      max_iterations: '最大迭代次数',
    },
  },
  {
    name: 'OR-Tools',
    code: 'ORTOOLS',
    category: '精确求解',
    problemTypes: ['LP', 'MILP', 'MIQP'],
    description: 'Google 运筹优化套件，集成多种求解器，支持线性规划、混合整数规划与二次约束。',
    params: {
      solver: '选择底层求解器 (GLOP/SCIP/CP-SAT)',
      time_limit: '求解时间限制 (秒)',
    },
    helpSections: [
      {
        title: '概述',
        content: 'GLOP、SCIP、CP-SAT 都是 Google OR-Tools 套件中的核心组件，但它们的设计目标、适用问题和底层算法完全不同。选择哪一个取决于你的问题类型和约束性质。',
      },
      {
        title: '🟦 GLOP (Google Linear Optimization Package)',
        content: '定位：Google 自研的纯线性规划求解器。\n\n特点：\n• 只能处理连续变量的线性目标函数和线性约束\n• 不包含任何整数规划功能\n• 在 OR-Tools 中常作为 MIP 求解器的线性松弛子求解器使用\n\n适用场景：\n• 网络流问题、运输问题的线性松弛\n• 需要极快求解大规模 LP 的场景\n• 验证 MIP 问题的线性下界\n\n⚠️ 注意：如果你的问题包含整数变量或 MakeIntVar，GLOP 无法直接使用。',
      },
      {
        title: '🟧 SCIP (Solving Constraint Integer Programs)',
        content: '定位：世界顶级的开源混合整数规划求解器，学术界公认最强非商业 MIP 求解器之一。\n\n特点：\n• 基于 Branch-and-Bound 框架，集成大量割平面（Cutting Planes）、启发式和预处理技术\n• 对标准 MIP 模型（如设施选址、背包、车辆路径的紧凑建模）表现优异\n• 支持部分非线性约束（MINLP）\n• OR-Tools 中通过 SCIP 接口调用（需单独安装或启用）\n\n适用场景：\n• 经典运筹学 MIP 问题\n• 数学结构清晰、可用线性不等式紧凑表达的问题\n• 需要与 Gurobi/CPLEX 对标比较的基准测试',
      },
      {
        title: '🟪 CP-SAT (Constraint Programming - SAT)',
        content: '定位：Google 自研的新一代约束编程+SAT混合求解器，目前 OR-Tools 官方最推荐的通用求解器。\n\n特点：\n• 底层是 SAT 求解器，将约束转化为布尔逻辑进行传播和搜索\n• 原生支持丰富的全局约束：AllDifferent, Cumulative, NoOverlap, Circuit, Table 等\n• 多求解器协同：内部同时运行 CP、SAT、LP 松弛等多个子求解器，共享信息\n• 对可行解搜索极其高效，即使找不到最优解也能快速给出高质量可行解\n• 近年来在 MIP 上也取得了惊人进展，许多传统 MIP 问题上已超越 SCIP\n\n适用场景：\n• 调度与排班（Job Shop, Flow Shop, Rostering）\n• 含复杂逻辑约束的问题（if-then, 计数, 序列依赖）\n• 组合优化、图着色、数独类问题\n• 难以用线性不等式紧凑建模的问题',
      },
      {
        title: '选型决策树',
        content: '你的问题是什么类型？\n\n├─ 纯连续变量 + 线性约束/目标 → ✅ GLOP\n├─ 含整数变量？\n│   ├─ 约束主要是线性不等式，数学结构紧凑\n│   │   ├─ 需要 MINLP 支持 → ✅ SCIP\n│   │   └─ 纯 MILP → 🔶 CP-SAT 或 SCIP（建议两者都试）\n│   └─ 含复杂逻辑/全局约束/调度/排班 → ✅ CP-SAT（强烈推荐）\n└─ 不确定 → ✅ 默认选 CP-SAT（它是 OR-Tools 当前发展重心，覆盖面最广）',
      },
      {
        title: '实践建议',
        content: '• 默认首选 CP-SAT：除非有明确理由用其他求解器，否则从 CP-SAT 开始。Google 已将大部分开发资源投入 CP-SAT，其 MIP 性能每年都在大幅提升。\n• MIP 问题做 A/B 测试：对于纯 MILP 问题，CP-SAT 和 SCIP 各有胜负。建议用同一模型分别跑两个求解器，设置相同时间上限比较结果。\n• GLOP 用于预热/分解：在大型 MIP 中，可先用 GLOP 求解线性松弛获取下界或生成初始解，再喂给 CP-SAT/SCIP。\n• 建模方式影响巨大：用 CP-SAT 时尽量使用全局约束而非线性化展开；用 SCIP 时尽量提供紧致的线性公式和有效的割平面提示。\n• Python API 差异：CP-SAT 使用 cp_model.CpModel()，而 SCIP/GLOP 通过 pywraplp.Solver.CreateSolver() 调用，API 风格不同，切换时需重写模型代码。',
      },
      {
        title: '总结',
        content: 'LP 用 GLOP，经典紧凑 MIP 试 SCIP，调度/逻辑/通用优化无脑选 CP-SAT。',
      },
    ],
  },
  {
    name: 'HiGHS',
    code: 'HIGHS',
    category: '精确求解',
    problemTypes: ['LP', 'MILP'],
    description: '高性能开源求解器，支持线性规划与混合整数规划，求解速度快，内存占用低。',
    params: {
      presolve: '是否启用预处理',
      time_limit: '求解时间限制 (秒)',
    },
  },
  {
    name: 'SciPy',
    code: 'SCIPY',
    category: '精确求解',
    problemTypes: ['LP', 'NLP'],
    description: 'Python 科学计算库优化模块，支持线性规划与非线性优化，接口简洁易用。',
    params: {
      method: '求解方法 (highs/revised simplex)',
      options: '求解器特定选项',
    },
  },
  {
    name: 'PuLP',
    code: 'PULP',
    category: '精确求解',
    problemTypes: ['LP', 'MILP'],
    description: 'Python 线性规划建模库，支持多种商业求解器桥接（CBC/Gurobi/CPLEX 等）。',
    params: {
      solver: '选择后端求解器 (CBC/Gurobi/CPLEX)',
      time_limit: '求解时间限制 (秒)',
    },
  },
  {
    name: 'ECOS',
    code: 'ECOS',
    category: '精确求解',
    problemTypes: ['LP', 'SOCP', 'MISOCP'],
    description: '嵌入式锥优化求解器，支持线性规划、二阶锥规划（SOCP）与混合整数二阶锥规划（MISOCP），适合中大规模锥优化问题。',
    params: {
      max_iters: '最大迭代次数',
      abstol: '绝对容差',
      reltol: '相对容差',
    },
  },
  {
    name: 'OSQP',
    code: 'OSQP',
    category: '精确求解',
    problemTypes: ['QP', 'MIQP', 'MIQCP'],
    description: '运算符分裂二次规划求解器，支持凸二次规划（QP）、混合整数凸二次规划（MIQP）与混合整数凸二次约束规划（MIQCP），适合大规模稀疏问题。',
    params: {
      max_iter: '最大迭代次数',
      eps_abs: '绝对精度',
      eps_rel: '相对精度',
      time_limit: '求解时间限制 (秒)',
    },
  },
  {
    name: 'CVXPY',
    code: 'CVXPY',
    category: '精确求解',
    problemTypes: ['LP', 'QP', 'SOCP', 'MILP', 'MIQP', 'MISOCP'],
    description: '凸优化建模框架，统一接口支持 LP/QP/SOCP/SDP 及其混合整数变体，自动选择最优后端求解器。',
    params: {
      solver: '后端求解器 (ECOS/OSQP/SCS/GUROBI/MOSEK)',
      verbose: '是否输出详细日志',
      time_limit: '求解时间限制 (秒)',
    },
  },
]

// 自定义算法
const customAlgorithms = ref<AlgorithmInfo[]>([])

// 所有算法（内置+自定义）
const allAlgorithms = computed(() => [...algorithms, ...customAlgorithms.value])

// OR-Tools 三大求解器对比表数据
const ortoolsCompareData = [
  { feature: '全称', glop: 'Google Linear Optimization Package', scip: 'Solving Constraint Integer Programs', cpsat: 'Constraint Programming - SAT' },
  { feature: '问题类型', glop: '纯线性规划 (LP)', scip: '混合整数规划 (MIP/MILP)', cpsat: '约束满足/优化 (CSP/COP) + SAT' },
  { feature: '变量类型', glop: '仅连续变量', scip: '连续 + 整数 + 二元', cpsat: '仅整数/布尔 (可编码连续)' },
  { feature: '核心算法', glop: 'Simplex / Barrier', scip: 'Branch-and-Bound + Cutting Planes', cpsat: 'Boolean Satisfiability + Propagation' },
  { feature: '优势领域', glop: '大规模线性松弛、预处理', scip: '传统运筹学MIP、学术基准', cpsat: '调度、排班、逻辑约束、组合优化' },
  { feature: '非线性支持', glop: '❌ 不支持', scip: '⚠️ 有限支持 (MINLP)', cpsat: '❌ 不支持 (需线性化)' },
  { feature: '并行能力', glop: '弱 (主要单线程)', scip: '强 (多线程 B&B)', cpsat: '极强 (多线程搜索 + 子求解器协同)' },
  { feature: '许可证', glop: 'Apache 2.0', scip: 'Apache 2.0 (OR-Tools内置版)', cpsat: 'Apache 2.0' },
]

const configDialogVisible = ref(false)
const helpDialogVisible = ref(false)
const selectedAlgorithm = ref<AlgorithmInfo | null>(null)
const configForm = ref<Record<string, any>>({})

async function openConfigDialog(algo: AlgorithmInfo) {
  selectedAlgorithm.value = algo
  configForm.value = {}
  // 初始化默认值
  if (algo.params) {
    Object.keys(algo.params).forEach(key => {
      configForm.value[key] = ''
    })
  }
  // 加载已保存的配置
  try {
    const savedConfig = await algorithmApi.getConfig(algo.code) as Record<string, any> | null
    if (savedConfig) {
      Object.keys(savedConfig).forEach(key => {
        if (key in configForm.value) {
          configForm.value[key] = savedConfig[key]
        }
      })
    }
  } catch {
    // 未保存过配置，使用默认空值
  }
  configDialogVisible.value = true
}

function showHelp(algo: AlgorithmInfo) {
  selectedAlgorithm.value = algo
  helpDialogVisible.value = true
}

async function saveConfig() {
  if (!canEditAlgorithm.value) {
    ElMessage.warning('只读模式下无法修改算法配置')
    return
  }
  const hasEmpty = Object.values(configForm.value).some(v => v === '')
  if (hasEmpty) {
    ElMessage.warning(t('message.pleaseFillAllParameters'))
    return
  }
  try {
    await algorithmApi.saveConfig(selectedAlgorithm.value!.code, configForm.value)
    ElMessage.success(t('message.configSaved'))
    configDialogVisible.value = false
  } catch {
    ElMessage.error('保存失败')
  }
}

// 求解能力声明
const solverCapabilities = [
  { label: 'LP', desc: '线性规划', ready: true },
  { label: 'MILP', desc: '混合整数线性规划', ready: true },
  { label: 'QP', desc: '凸二次规划', ready: true },
  { label: 'MIQP', desc: '混合整数凸二次规划', ready: true },
  { label: 'SOCP', desc: '二阶锥规划', ready: true },
  { label: 'MISOCP', desc: '混合整数二阶锥规划', ready: true },
  { label: 'MIQCP', desc: '混合整数凸二次约束规划', ready: true },
  { label: 'NLP', desc: '非线性规划', ready: true },
  { label: 'MINLP', desc: '混合整数非线性规划', ready: true },
]

// 自定义算法注册
const registerDialogVisible = ref(false)
const registerForm = ref({
  name: '',
  code: '',
  category: '自定义',
  description: '',
  integrationType: 'api' as 'api' | 'script',
  apiEndpoint: '',
  scriptContent: '',
  problemTypes: [] as string[],
  params: [] as { key: string; desc: string }[],
})

const problemTypeOptions = [
  'LP', 'MILP', 'QP', 'MIQP', 'SOCP', 'MISOCP', 'MIQCP', 'NLP', 'MINLP',
]

const uploadedFileName = ref('')

function openRegisterDialog() {
  registerForm.value = {
    name: '',
    code: '',
    category: '自定义',
    description: '',
    integrationType: 'api',
    apiEndpoint: '',
    scriptContent: '',
    problemTypes: [],
    params: [],
  }
  uploadedFileName.value = ''
  registerDialogVisible.value = true
}

function handleScriptUpload(file: File): boolean {
  if (!file.name.endsWith('.py')) {
    ElMessage.warning('请上传 .py 格式的 Python 脚本文件')
    return false
  }
  if (file.size > 1024 * 1024) {
    ElMessage.warning('脚本文件不能超过 1MB')
    return false
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    registerForm.value.scriptContent = (e.target?.result as string) || ''
    uploadedFileName.value = file.name
    ElMessage.success(`已读取脚本文件：${file.name}`)
  }
  reader.onerror = () => {
    ElMessage.error('文件读取失败')
  }
  reader.readAsText(file)
  return false
}

function addParam() {
  registerForm.value.params.push({ key: '', desc: '' })
}

function removeParam(index: number) {
  registerForm.value.params.splice(index, 1)
}

async function saveCustomAlgorithm() {
  if (!canEditAlgorithm.value) {
    ElMessage.warning('只读模式下无法注册自定义算法')
    return
  }
  const form = registerForm.value
  if (!form.name || !form.code) {
    ElMessage.warning('请填写算法名称和编码')
    return
  }
  if (form.problemTypes.length === 0) {
    ElMessage.warning('请选择至少一个适用问题类型')
    return
  }
  if (form.integrationType === 'api' && !form.apiEndpoint) {
    ElMessage.warning('请填写 API 端点地址')
    return
  }
  if (form.integrationType === 'script' && !form.scriptContent) {
    ElMessage.warning('请填写或上传 Python 脚本')
    return
  }

  const paramsObj: Record<string, string> = {}
  form.params.forEach(p => {
    if (p.key) paramsObj[p.key] = p.desc
  })

  const newAlgo: AlgorithmInfo = {
    name: form.name,
    code: form.code.toUpperCase(),
    category: '自定义',
    problemTypes: form.problemTypes,
    description: form.description,
    params: Object.keys(paramsObj).length > 0 ? paramsObj : undefined,
  }

  try {
    await algorithmApi.registerCustom({
      name: form.name,
      code: form.code.toUpperCase(),
      description: form.description,
      problemTypes: form.problemTypes,
      integrationType: form.integrationType,
      apiEndpoint: form.apiEndpoint,
      scriptContent: form.scriptContent,
      params: paramsObj,
    })
    customAlgorithms.value.push(newAlgo)
    ElMessage.success(`自定义算法 "${form.name}" 注册成功`)
    registerDialogVisible.value = false
  } catch {
    ElMessage.error('注册失败')
  }
}

async function loadCustomAlgorithms() {
  try {
    const list = await algorithmApi.listCustom()
    customAlgorithms.value = (list || []).map((item: any) => ({
      name: item.name,
      code: item.code,
      category: '自定义',
      problemTypes: item.problemTypes ? item.problemTypes.split(',') : [],
      description: item.description || '',
      params: item.params ? (typeof item.params === 'string' ? JSON.parse(item.params) : item.params) : undefined,
    }))
  } catch {
    // Backend not available, ignore
  }
}

loadCustomAlgorithms()
</script>

<style scoped>
.algo-card {
  margin-bottom: 20px;
}
.algo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.algo-name {
  font-weight: 600;
  font-size: 16px;
}
.algo-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0 0 12px;
  line-height: 1.6;
}
.algo-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.algo-type-tag {
  font-weight: 500;
}
.algo-params {
  margin-top: 8px;
}
.algo-param-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}
.param-key {
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
}
.param-desc {
  color: var(--el-text-color-secondary);
}
.algo-code {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.algo-code .code-label {
  margin-right: 4px;
}
.algo-code code {
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
}
.algo-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
.help-icon {
  margin-left: 8px;
  color: var(--el-color-info);
  cursor: pointer;
}
.help-icon:hover {
  color: var(--el-color-primary);
}
.help-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.capability-card {
  margin-bottom: 20px;
}
.capability-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.capability-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cap-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.param-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.5;
}
.script-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.script-upload {
  display: inline-block;
}
.uploaded-file {
  font-size: 12px;
  color: var(--el-color-success);
}
.help-dialog-body {
  max-height: 70vh;
  overflow-y: auto;
}
.help-section {
  margin-top: 20px;
}
.help-section h4 {
  margin-bottom: 10px;
  font-size: 15px;
  color: var(--el-text-color-primary);
}
.help-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.8;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
  padding: 14px 16px;
  border-radius: 8px;
  margin: 0;
}
.compare-table-wrapper {
  margin-top: 16px;
}
</style>
