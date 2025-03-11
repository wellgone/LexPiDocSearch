import Cookies from 'js-cookie';

/** 保存token值的key名 */
export const TOKEN_KEY = 'token';
export const UID_KEY = 'uid';

/** 读取token */
export function getToken(): string | null {
  return Cookies.get(TOKEN_KEY) || null;
}

/** 设置token */
export function setToken(token: string): void {
  const cookieOptions = {
    expires: 7, // token有效期7天
    path: '/',
    sameSite: 'strict' as const,
    secure: window.location.protocol === 'https:',
  };

  // 同时设置token和uid
  Cookies.set(TOKEN_KEY, token, cookieOptions);
  Cookies.set(UID_KEY, token, cookieOptions);
}

/** 删除token */
export function removeToken(): void {
  const cookieOptions = {
    path: '/',
    sameSite: 'strict' as const,
    secure: window.location.protocol === 'https:',
  };

  Cookies.remove(TOKEN_KEY, cookieOptions);
  Cookies.remove(UID_KEY, cookieOptions);
}

/** 检查是否有token */
export function hasToken(): boolean {
  return !!getToken();
} 