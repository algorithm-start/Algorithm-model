<template>
  <div class="workspace-page">
    <div class="page-header">
      <div>
        <h2>空间管理</h2>
        <p class="page-desc">管理工作空间，空间内的数据对所有成员可见</p>
      </div>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon> 创建空间
      </el-button>
    </div>

    <el-table :data="workspaces" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ownerName" label="创建者" width="100" />
      <el-table-column prop="memberCount" label="成员数" width="80" align="center" />
      <el-table-column prop="myRole" label="我的角色" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.myRole === 'ADMIN'" type="warning" size="small">管理员</el-tag>
          <el-tag v-else-if="row.myRole === 'MEMBER'" size="small">成员</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openMembers(row)">成员</el-button>
          <el-button link type="primary" @click="editWorkspace(row)">编辑</el-button>
          <el-button link type="danger" @click="deleteWorkspace(row)" :disabled="row.code === 'default'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑空间' : '创建空间'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="请输入空间名称" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="form.code" placeholder="唯一标识，如 team-alpha" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="空间描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- Members Dialog -->
    <el-dialog v-model="membersDialogVisible" :title="`${currentWorkspace?.name} - 成员管理`" width="600px">
      <div class="members-toolbar">
        <el-button type="primary" size="small" @click="showAddMemberDialog">
          <el-icon><Plus /></el-icon> 添加成员
        </el-button>
      </div>
      <el-table :data="members" v-loading="membersLoading" size="small">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'ADMIN'" type="warning" size="small">管理员</el-tag>
            <el-tag v-else size="small">成员</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link size="small" type="primary" @click="toggleRole(row)">
              {{ row.role === 'ADMIN' ? '设为成员' : '设为管理员' }}
            </el-button>
            <el-button link size="small" type="danger" @click="removeMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Add Member Dialog -->
    <el-dialog v-model="addMemberDialogVisible" title="添加成员" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-select v-model="addMemberForm.userIds" multiple filterable placeholder="选择用户" style="width: 100%">
            <el-option
              v-for="user in availableUsers"
              :key="user.id"
              :label="user.username + (user.nickname ? ` (${user.nickname})` : '')"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="addMemberForm.role">
            <el-radio value="MEMBER">成员</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMemberDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddMembers" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { workspaceApi, type Workspace, type WorkspaceMember } from '@/api/workspace'
import request from '@/api/index'

const loading = ref(false)
const workspaces = ref<Workspace[]>([])
const dialogVisible = ref(false)
const editingId = ref<string>('')
const submitting = ref(false)
const form = ref({ name: '', code: '', description: '' })

// Members
const membersDialogVisible = ref(false)
const membersLoading = ref(false)
const currentWorkspace = ref<Workspace | null>(null)
const members = ref<WorkspaceMember[]>([])

// Add member
const addMemberDialogVisible = ref(false)
const addMemberForm = ref({ userIds: [] as string[], role: 'MEMBER' })
const availableUsers = ref<any[]>([])

onMounted(() => {
  loadWorkspaces()
})

async function loadWorkspaces() {
  loading.value = true
  try {
    workspaces.value = await workspaceApi.listAll()
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  editingId.value = ''
  form.value = { name: '', code: '', description: '' }
  dialogVisible.value = true
}

function editWorkspace(row: Workspace) {
  editingId.value = row.id
  form.value = { name: row.name, code: row.code, description: row.description || '' }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.name || !form.value.code) {
    ElMessage.warning('请填写名称和编码')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await workspaceApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await workspaceApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadWorkspaces()
  } finally {
    submitting.value = false
  }
}

async function deleteWorkspace(row: Workspace) {
  await ElMessageBox.confirm(`确定删除空间「${row.name}」？空间内的数据不会被删除。`, '确认')
  await workspaceApi.delete(row.id)
  ElMessage.success('删除成功')
  loadWorkspaces()
}

async function openMembers(row: Workspace) {
  currentWorkspace.value = row
  membersDialogVisible.value = true
  membersLoading.value = true
  try {
    members.value = await workspaceApi.getMembers(row.id)
  } finally {
    membersLoading.value = false
  }
}

async function showAddMemberDialog() {
  addMemberForm.value = { userIds: [], role: 'MEMBER' }
  // Load all users
  try {
    const res = await request.get('/iam/users', { params: { page: 1, size: 100 } }) as any
    const records = res?.records || res || []
    // Filter out existing members
    const memberUserIds = new Set(members.value.map(m => m.userId))
    availableUsers.value = records.filter((u: any) => !memberUserIds.has(u.id))
  } catch {
    availableUsers.value = []
  }
  addMemberDialogVisible.value = true
}

async function confirmAddMembers() {
  if (addMemberForm.value.userIds.length === 0) {
    ElMessage.warning('请选择用户')
    return
  }
  submitting.value = true
  try {
    await workspaceApi.addMembers(currentWorkspace.value!.id, {
      userIds: addMemberForm.value.userIds,
      role: addMemberForm.value.role,
    })
    ElMessage.success('添加成功')
    addMemberDialogVisible.value = false
    members.value = await workspaceApi.getMembers(currentWorkspace.value!.id)
    loadWorkspaces()
  } finally {
    submitting.value = false
  }
}

async function toggleRole(row: WorkspaceMember) {
  const newRole = row.role === 'ADMIN' ? 'MEMBER' : 'ADMIN'
  try {
    await workspaceApi.setMemberRole(currentWorkspace.value!.id, row.userId, newRole)
    ElMessage.success('角色已更新')
    members.value = await workspaceApi.getMembers(currentWorkspace.value!.id)
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function removeMember(row: WorkspaceMember) {
  await ElMessageBox.confirm(`确定移除成员「${row.username}」？`, '确认')
  await workspaceApi.removeMember(currentWorkspace.value!.id, row.userId)
  ElMessage.success('已移除')
  members.value = await workspaceApi.getMembers(currentWorkspace.value!.id)
  loadWorkspaces()
}

function formatTime(time: string) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.workspace-page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.page-desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.members-toolbar {
  margin-bottom: 12px;
}
</style>
