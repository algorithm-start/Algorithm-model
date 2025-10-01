import request from './index'

export interface SourceParams {
  page?: number
  size?: number
  type?: string
}

export interface SourceCreateData {
  name: string
  type: string
  connectionConfig: Record<string, unknown>
  description?: string
}

export interface PipelineParams {
  page?: number
  size?: number
  status?: string
}

export interface PipelineNodeDef {
  id: string
  type: string
  subType: string
  name: string
  config: Record<string, unknown>
  position?: { x: number; y: number }
}

export interface PipelineEdgeDef {
  id: string
  source: string
  target: string
}

export interface PipelineDefinition {
  nodes: PipelineNodeDef[]
  edges: PipelineEdgeDef[]
}

export interface PipelineCreateData {
  name: string
  description?: string
  definition: PipelineDefinition
  mode?: string
  schedule?: string
}

export interface DataSourceColumn {
  name: string
  type: string
  size: number
  nullable: boolean
}

export interface DataSourceTable {
  name: string
  schema?: string
  type?: string
  columns: DataSourceColumn[]
}

export interface DataSourceMetadata {
  databaseProductName?: string
  databaseProductVersion?: string
  tables: DataSourceTable[]
}

export interface ApiParams {
  page?: number
  size?: number
}

export interface QueryRequest {
  dataSourceId: number | string
  query: string
  limit?: number
}

export const dataApi = {
  listSources: (params: SourceParams) => request.get('/data/sources', { params }),
  getSource: (id: string) => request.get(`/data/sources/${id}`),
  getSourceMetadata: (id: string | number) =>
    request.get<unknown, DataSourceMetadata>(`/data/sources/${id}/metadata`),
  createSource: (data: SourceCreateData) => request.post('/data/sources', data),
  updateSource: (id: string, data: Partial<SourceCreateData>) => request.put(`/data/sources/${id}`, data),
  deleteSource: (id: string) => request.delete(`/data/sources/${id}`),
  testConnection: (id: string) => request.post(`/data/sources/${id}/test`),
  testConnectionByConfig: (data: SourceCreateData) => request.post('/data/sources/test', data),

  listPipelines: (params: PipelineParams) => request.get('/data/pipelines', { params }),
  getPipeline: (id: string) => request.get(`/data/pipelines/${id}`),
  createPipeline: (data: PipelineCreateData) => request.post('/data/pipelines', data),
  updatePipeline: (id: string, data: Partial<PipelineCreateData>) => request.put(`/data/pipelines/${id}`, data),
  deletePipeline: (id: string) => request.delete(`/data/pipelines/${id}`),
  executePipeline: (id: string) => request.post(`/data/pipelines/${id}/execute`),

  listApis: (params: ApiParams) => request.get('/data/apis', { params }),
  getApi: (id: string) => request.get(`/data/apis/${id}`),
  publishApi: (id: string) => request.post(`/data/apis/${id}/publish`),
  unpublishApi: (id: string) => request.post(`/data/apis/${id}/unpublish`),

  executeQuery: (data: QueryRequest) => request.post('/data/query', data),
}
