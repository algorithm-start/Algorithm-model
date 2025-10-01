import request from './index'

export interface ProblemParams {
  page?: number
  size?: number
  keyword?: string
  status?: string
}

export interface VariableDef {
  name: string
  type: 'CONTINUOUS' | 'INTEGER' | 'BINARY'
  lowerBound?: number
  upperBound?: number
}

export interface ConstraintDef {
  name: string
  expression: string
  type: 'LEQ' | 'EQ' | 'GEQ'
  rhs: number
}

export interface ObjectiveDef {
  expression: string
  sense: 'MINIMIZE' | 'MAXIMIZE'
}

export interface ProblemCreateData {
  problemName: string
  description?: string
  problemDefinition: {
    variables: VariableDef[]
    constraints: ConstraintDef[]
    objective: ObjectiveDef
    algorithmId?: string
    algorithmParams?: Record<string, unknown>
  }
}

export interface ExampleItem {
  id: string
  name: string
  type: string
  description?: string
}

export interface ExampleDetail {
  id: string
  name: string
  type: string
  description?: string
  problem: ProblemCreateData
}

export const solverApi = {
  listProblems: (params: ProblemParams) => request.get('/solver/problems', { params }),
  createProblem: (data: ProblemCreateData) => request.post('/solver/problems', data),
  getProblem: (id: string) => request.get(`/solver/problems/${id}`),
  solveProblem: (id: string, config?: Record<string, unknown>) =>
    request.post(`/solver/problems/${id}/solve`, config || {}),
  deleteProblem: (id: string) => request.delete(`/solver/problems/${id}`),
  listAlgorithms: () => request.get('/solver/algorithms'),
  getAlgorithm: (id: string) => request.get(`/solver/algorithms/${id}`),
  autoSelect: (data: Record<string, unknown>) => request.post('/solver/auto-select', data),
}

export const algorithmApi = {
  getConfig: (code: string) => request.get(`/solver/algorithm-configs/${code}`),
  saveConfig: (code: string, data: Record<string, unknown>) =>
    request.post(`/solver/algorithm-configs/${code}`, data),
  listCustom: () => request.get<unknown, any[]>('/solver/custom-algorithms'),
  registerCustom: (data: Record<string, unknown>) =>
    request.post('/solver/custom-algorithms', data),
  deleteCustom: (id: string | number) =>
    request.delete(`/solver/custom-algorithms/${id}`),
}

// Examples are persisted in the backend database and served via the solver module.
// The shared `request` instance already unwraps the { code, message, data } envelope.
export const examplesApi = {
  getExamples: async (): Promise<{ data: ExampleItem[] }> => {
    const data = await request.get<unknown, ExampleItem[]>('/solver/examples')
    return { data: data || [] }
  },
  getExample: async (id: string): Promise<{ data: ExampleDetail }> => {
    const data = await request.get<unknown, ExampleDetail>(`/solver/examples/${id}`)
    return { data }
  },
}
