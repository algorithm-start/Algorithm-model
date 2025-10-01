import request from './index'

export interface AuditLogParams {
  page?: number
  size?: number
  startTime?: string
  endTime?: string
  username?: string
  action?: string
  resource?: string
  result?: string
}

export const auditApi = {
  listLogs: (params: AuditLogParams) => request.get('/audit/logs', { params }),
  getLog: (id: string) => request.get(`/audit/logs/${id}`),
  exportLogs: (params: AuditLogParams) =>
    request.get('/audit/logs/export', { params, responseType: 'blob' } as any),
}
