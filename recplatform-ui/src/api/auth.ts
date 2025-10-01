import request from './index'

export interface LoginData {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  refreshToken?: string
}

export interface UserInfo {
  id: string
  username: string
  nickname: string
  email: string
  roles: any[]
  avatar?: string
  /** Merged menu keys from all user roles. */
  menus?: string[]
}

export const authApi = {
  login: (data: LoginData) => request.post<unknown, LoginResult>('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  getUserInfo: () => request.get<unknown, UserInfo>('/auth/userinfo'),
  refreshToken: (refreshToken: string) =>
    request.post<unknown, LoginResult>('/auth/refresh', { refreshToken }),
}
