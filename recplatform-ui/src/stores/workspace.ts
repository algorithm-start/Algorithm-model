import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { workspaceApi, type Workspace } from '@/api/workspace'

export const useWorkspaceStore = defineStore('workspace', () => {
  const workspaces = ref<Workspace[]>([])
  const currentWorkspaceId = ref<string>(localStorage.getItem('currentWorkspaceId') || '')

  const currentWorkspace = computed(() =>
    workspaces.value.find(w => w.id === currentWorkspaceId.value) || workspaces.value[0]
  )

  async function loadWorkspaces() {
    try {
      workspaces.value = await workspaceApi.listMine()
      resolveDefaultWorkspace()
    } catch {
      workspaces.value = []
    }
  }

  /**
   * Decide which workspace to enter after login, by priority:
   *   1. The last selected workspace, if it is still accessible.
   *   2. Otherwise, the workspace where the user is ADMIN (first one if many).
   *   3. Otherwise, the first workspace in the list.
   */
  function resolveDefaultWorkspace() {
    if (workspaces.value.length === 0) {
      return
    }

    const lastSelectedStillAccessible = currentWorkspaceId.value &&
      workspaces.value.some(w => w.id === currentWorkspaceId.value)
    if (lastSelectedStillAccessible) {
      return
    }

    const ownAdminWorkspace = workspaces.value.find(w => w.myRole === 'ADMIN')
    if (ownAdminWorkspace) {
      setCurrentWorkspace(ownAdminWorkspace.id)
      return
    }

    setCurrentWorkspace(workspaces.value[0].id)
  }

  function setCurrentWorkspace(id: string) {
    currentWorkspaceId.value = id
    localStorage.setItem('currentWorkspaceId', id)
  }

  function clear() {
    workspaces.value = []
    currentWorkspaceId.value = ''
    localStorage.removeItem('currentWorkspaceId')
  }

  return {
    workspaces,
    currentWorkspaceId,
    currentWorkspace,
    loadWorkspaces,
    setCurrentWorkspace,
    clear,
  }
})
