import request from './index'

export interface FlowParams {
  page?: number
  size?: number
  status?: string
}

export interface FlowNodeDef {
  id: string
  type: string
  name: string
  config: Record<string, unknown>
  position?: { x: number; y: number }
}

export interface FlowEdgeDef {
  id: string
  source: string
  target: string
  condition?: string
}

export interface FlowDefinitionData {
  nodes: FlowNodeDef[]
  edges: FlowEdgeDef[]
}

export interface FlowCreateData {
  name: string
  description?: string
  definition: FlowDefinitionData
  category?: string
  tags?: string[]
}

export const orchestratorApi = {
  listFlows: (params: FlowParams) => request.get('/orchestrator/flows', { params }),
  getFlow: (id: string) => request.get(`/orchestrator/flows/${id}`),
  createFlow: (data: FlowCreateData) => request.post('/orchestrator/flows', data),
  updateFlow: (id: string, data: Partial<FlowCreateData>) => request.put(`/orchestrator/flows/${id}`, data),
  deleteFlow: (id: string) => request.delete(`/orchestrator/flows/${id}`),
  publishFlow: (id: string) => request.post(`/orchestrator/flows/${id}/publish`),

  executeFlow: (id: string, params?: Record<string, unknown>) =>
    request.post(`/orchestrator/flows/${id}/execute`, params || {}),
  listExecutions: (params: { flowId?: string; page?: number; size?: number }) =>
    request.get('/orchestrator/executions', { params }),
  getExecution: (id: string) => request.get(`/orchestrator/executions/${id}`),
  cancelExecution: (id: string) => request.post(`/orchestrator/executions/${id}/cancel`),
}
