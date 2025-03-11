import request from '@/utils/request';
import { setToken, removeToken } from '@/utils/auth';

export interface LoginParams {
  userAccount: string;
  userPassword: string;
}

export interface UserInfo {
  userId: number;
  userAccount: string;
  userName: string;
  userEmail: string;
  userRole: string;
  userState: number;
  userLoginNum: number;
  createTime: string;
  modifiedTime: string;
}

export type { UserInfo as APIUserInfo };

export interface LoginResult {
  code: number;
  data: {
    user: UserInfo;
    token: string;
    tokenTimeout: number;
    loginTime: string;
  };
  message: string;
}

export async function login(params: LoginParams): Promise<LoginResult> {
  const response = await request('/api/user/login', {
    method: 'POST',
    data: params,
  });
  
  if (response.code === 0 && response.data.token) {
    setToken(response.data.token);
  }
  
  return response;
}

export async function logout(): Promise<void> {
  try {
    await request('/api/user/logout', {
      method: 'POST',
    });
  } finally {
    removeToken();
  }
}

export async function getCurrentUser(): Promise<UserInfo> {
  const response = await request('/api/user/current', {
    method: 'GET',
  });
  return response.data;
}

export async function checkToken(): Promise<boolean> {
  try {
    const response = await getCurrentUser();
    return !!response;
  } catch (error) {
    return false;
  }
} 