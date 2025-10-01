import request from './index'

export interface Workspace {
  id: string
  name: string
  code: string
  description: string
  ownerId: string
  ownerName: string
  memberCount: number
  myRole: string
  createTime: string
}

export interface WorkspaceMember {
  id: string
  userId: string
  username: string
  nickname: string
  role: string
  createTime: string
}

export interface WorkspaceCreateRequest {
  name: string
  code: string
  description?: string
}

export interface WorkspaceMemberRequest {
  userIds: string[]
  role?: string
}

export const workspaceApi = {
  listMine: () => request.get('/workspaces/mine') as Promise<Workspace[]>,

  listAll: () => request.get('/workspaces') as Promise<Workspace[]>,

  getById: (id: string) => request.get(`/workspaces/${id}`) as Promise<Workspace>,

  create: (data: WorkspaceCreateRequest) => request.post('/workspaces', data) as Promise<Workspace>,

  update: (id: string, data: Partial<WorkspaceCreateRequest>) =>
    request.put(`/workspaces/${id}`, data) as Promise<Workspace>,

  delete: (id: string) => request.delete(`/workspaces/${id}`) as Promise<void>,

  getMembers: (id: string) => request.get(`/workspaces/${id}/members`) as Promise<WorkspaceMember[]>,

  addMembers: (id: string, data: WorkspaceMemberRequest) =>
    request.post(`/workspaces/${id}/members`, data) as Promise<void>,

  removeMember: (workspaceId: string, userId: string) =>
    request.delete(`/workspaces/${workspaceId}/members/${userId}`) as Promise<void>,

  setMemberRole: (workspaceId: string, userId: string, role: string) =>
    request.put(`/workspaces/${workspaceId}/members/${userId}/role`, null, { params: { role } }) as Promise<void>,
}
