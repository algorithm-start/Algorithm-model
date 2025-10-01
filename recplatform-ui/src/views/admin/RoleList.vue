<template>
  <div class="role-list">
    <PageHeader :title="t('menu.roles')" :subtitle="t('admin.manageRoles')">
      <el-button type="primary" @click="showDialog()">
        <el-icon><Plus /></el-icon>
        {{ t('button.addRole') }}
      </el-button>
    </PageHeader>

    <el-card>
      <el-table :data="roles" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="name" :label="t('table.name')" min-width="160" />
        <el-table-column prop="description" :label="t('form.description')" min-width="250" />
        <el-table-column prop="permissions" :label="t('admin.permissions')" min-width="300">
          <template #default="{ row }">
            <el-tag v-for="perm in (row.permissions || []).slice(0, 3)" :key="perm" size="small" style="margin: 2px;">
              {{ perm }}
            </el-tag>
            <el-tag v-if="(row.permissions || []).length > 3" size="small" type="info" style="margin: 2px;">
              +{{ row.permissions.length - 3 }} {{ t('admin.more') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('table.actions')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDialog(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingRole ? t('button.editRole') : t('button.addRole')" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('table.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('form.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('admin.permissions')">
          <el-checkbox-group v-model="form.permissions">
            <el-checkbox label="solver:read" value="solver:read" />
            <el-checkbox label="solver:write" value="solver:write" />
            <el-checkbox label="orchestrator:read" value="orchestrator:read" />
            <el-checkbox label="orchestrator:write" value="orchestrator:write" />
            <el-checkbox label="data:read" value="data:read" />
            <el-checkbox label="data:write" value="data:write" />
            <el-checkbox label="admin:read" value="admin:read" />
            <el-checkbox label="admin:write" value="admin:write" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="可见菜单">
          <el-tree
            ref="menuTreeRef"
            :data="menuTreeData"
            show-checkbox
            node-key="id"
            :default-checked-keys="form.menus"
            :props="{ label: 'label', children: 'children' }"
            @check="handleMenuCheck"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import request from '@/api/index'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const { t } = useI18n()
const roles = ref<any[]>([])
const dialogVisible = ref(false)
const editingRole = ref<any>(null)
const formRef = ref<FormInstance>()

const menuTreeRef = ref<any>(null)

const form = reactive({
  name: '',
  description: '',
  permissions: [] as string[],
  menus: [] as string[],
})

const menuTreeData = [
  { id: 'dashboard', label: '仪表盘' },
  {
    id: 'solver', label: '求解器',
    children: [
      { id: 'solver:problems', label: '问题管理' },
      { id: 'solver:algorithms', label: '算法管理' },
    ],
  },
  {
    id: 'orchestrator', label: '编排器',
    children: [
      { id: 'orchestrator:flows', label: '流程管理' },
      { id: 'orchestrator:executions', label: '执行记录' },
    ],
  },
  {
    id: 'data', label: '数据管理',
    children: [
      { id: 'data:sources', label: '数据源' },
      { id: 'data:pipelines', label: '数据管道' },
      { id: 'data:apis', label: 'API 管理' },
      { id: 'data:query', label: '数据查询' },
    ],
  },
  {
    id: 'admin', label: '系统管理',
    children: [
      { id: 'admin:users', label: '用户管理' },
      { id: 'admin:roles', label: '角色管理' },
      { id: 'admin:workspaces', label: '空间管理' },
      { id: 'admin:audit', label: '审计日志' },
      { id: 'admin:system', label: '系统信息' },
    ],
  },
]

function handleMenuCheck() {
  if (menuTreeRef.value) {
    const checked = menuTreeRef.value.getCheckedKeys(false) as string[]
    const halfChecked = menuTreeRef.value.getHalfCheckedKeys() as string[]
    form.menus = [...checked, ...halfChecked]
  }
}

/** Expand top-level menu keys to include all child keys for el-tree setCheckedKeys. */
function expandMenuKeysForTree(menus: string[]): string[] {
  const allKeys: string[] = []
  for (const key of menus) {
    allKeys.push(key)
    const node = menuTreeData.find(n => n.id === key)
    if (node && 'children' in node) {
      for (const child of (node as any).children) {
        allKeys.push(child.id)
      }
    }
  }
  return allKeys
}

const rules: FormRules = {
  name: [{ required: true, message: 'Required', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/iam/roles')
    roles.value = (res as any).records || (res as any).items || (res as any).list || []
  } catch {
    // 接口未实现，使用示例数据
    roles.value = [
      { id: '1', name: 'ADMIN', description: '系统管理员，拥有所有权限', permissions: ['solver:read', 'solver:write', 'orchestrator:read', 'orchestrator:write', 'data:read', 'data:write', 'admin:read', 'admin:write'] },
      { id: '2', name: 'ANALYST', description: '数据分析师，只读权限', permissions: ['solver:read', 'orchestrator:read', 'data:read'] },
      { id: '3', name: 'ENGINEER', description: '工程师，可执行权限', permissions: ['solver:read', 'solver:write', 'orchestrator:read', 'orchestrator:write', 'data:read'] },
    ]
  } finally { loading.value = false }
}

function showDialog(role?: any) {
  editingRole.value = role || null
  if (role) {
    Object.assign(form, {
      name: role.name || '',
      description: role.description || '',
      permissions: role.permissions || role.permissionCodes || [],
      menus: role.menus || [],
    })
  } else {
    Object.assign(form, { name: '', description: '', permissions: [], menus: [] })
  }
  dialogVisible.value = true
  // Set tree checked keys after dialog opens (nextTick for el-tree to mount)
  // Expand top-level keys to leaf keys so el-tree shows correct check state
  const expandedKeys = expandMenuKeysForTree(form.menus)
  setTimeout(() => {
    if (menuTreeRef.value) {
      menuTreeRef.value.setCheckedKeys(expandedKeys, false)
    }
  }, 100)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const payload = {
      name: form.name,
      description: form.description,
      menus: form.menus,
    }
    if (editingRole.value) {
      await request.put(`/iam/roles/${editingRole.value.id}`, payload)
    } else {
      await request.post('/iam/roles', { ...payload, code: form.name.toUpperCase().replace(/\s+/g, '_') })
    }
    ElMessage.success(t('message.roleSaved'))
    dialogVisible.value = false
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm(t('message.deleteRoleConfirm'), t('message.confirmTitle'), { type: 'warning' })
    await request.delete(`/iam/roles/${id}`)
    ElMessage.success(t('message.roleDeleted'))
    loadData()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>
