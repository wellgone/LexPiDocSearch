import { Footer, Question, AvatarDropdown, AvatarName } from '@/components';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { IRouteProps } from '@umijs/max';
import { history } from '@umijs/max';
import defaultSettings from '../config/defaultSettings';
import { errorConfig } from './requestErrorConfig';
import { getCurrentUser } from './services/auth';
import { getToken } from './utils/auth';
import type { UserInfo } from './services/auth';

const isDev = process.env.NODE_ENV === 'development';
const loginPath = '/user/login';

/**
 * @see  https://umijs.org/zh-CN/plugins/plugin-initial-state
 * */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  currentUser?: UserInfo;
  loading?: boolean;
  fetchUserInfo?: () => Promise<UserInfo | undefined>;
}> {
  const fetchUserInfo = async () => {
    try {
      const token = getToken();
      if (!token) {
        throw new Error('No token found');
      }

      const currentUser = await getCurrentUser();
      // 将用户ID持久化到localStorage
      if (currentUser.userId) {
        localStorage.setItem('userId', currentUser.userId.toString());
      }
      return currentUser;
    } catch (error) {
      history.push(loginPath);
    }
    return undefined;
  };

  // 如果不是登录页面，执行
  const { location } = history;
  if (location.pathname !== loginPath) {
    const currentUser = await fetchUserInfo();
    return {
      fetchUserInfo,
      currentUser,
      settings: defaultSettings as Partial<LayoutSettings>,
    };
  }
  return {
    fetchUserInfo,
    settings: defaultSettings as Partial<LayoutSettings>,
  };
}

// ProLayout 支持的api https://procomponents.ant.design/components/layout
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const layout = ({ initialState }: { initialState?: {
  settings?: Partial<LayoutSettings>;
  currentUser?: UserInfo;
} }) => {
  return {
    actionsRender: () => [<Question key="doc" />],
    avatarProps: {
      src: undefined,
      title: <AvatarName />,
      render: (_: unknown, avatarChildren: React.ReactNode) => {
        return <AvatarDropdown>{avatarChildren}</AvatarDropdown>;
      },
    },
    waterMarkProps: {
      content: "律π--知识管理",
    },
    footerRender: () => <Footer />,
    onPageChange: () => {
      const { location } = history;
      // 如果没有登录，重定向到 login
      if (!initialState?.currentUser && location.pathname !== loginPath) {
        history.push(loginPath);
      }
    },
    menuHeaderRender: undefined,
    ...initialState?.settings,
  };
};

/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request
 */
export const request = {
  ...errorConfig,
  requestInterceptors: [
    (config: any) => {
      // 拦截请求配置，进行个性化处理。
      const token = getToken();
      if (token) {
        const headers = {
          ...(config.headers || {}),
          'Authorization': `Bearer ${token}`,
          'token': token,
        };
        return { ...config, headers };
      }
      return config;
    }
  ],
};

// 路由守卫
export function onRouteChange({ location }: { location: any }) {
  const { pathname } = location;

  // 白名单路由，不需要登录
  const whiteList = [loginPath, '/user/register'];

  // 如果当前路径已经是登录页，则不需要再次重定向
  if (pathname === loginPath) {
    return;
  }

  if (!whiteList.includes(pathname)) {
    const token = getToken();
    if (!token) {
      history.push(loginPath);
    }
  }
}
