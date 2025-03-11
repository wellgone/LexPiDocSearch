import { extend } from 'umi-request';
import { message } from 'antd';
import { parseJSON, stringifyJSON } from './jsonBigint';

interface CodeMessageMap {
  [key: number]: string;
}

interface RequestHeaders {
  [key: string]: string;
}

const codeMessage: CodeMessageMap = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '用户没有权限（令牌、用户名、密码错误）。',
  403: '用户得到授权，但是访问是被禁止的。',
  404: '发出的请求针对的是不存在的记录，服务器没有进行操作。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。',
};

const request = extend({
  errorHandler: (error: any) => {
    const { response } = error;
    if (response && response.status) {
      const errorText = codeMessage[response.status] || response.statusText;
      const { status, url } = response;

      message.error(`请求错误 ${status}: ${url}\n${errorText}`);
    } else if (!response) {
      message.error('网络异常，无法连接服务器');
    }
    return response;
  },
});

// 请求拦截器
request.interceptors.request.use((url, options) => {
  const token = localStorage.getItem('token');
  const headers: RequestHeaders = {
    ...(options.headers as RequestHeaders || {}),
  };

  if (token) {
    headers.satoken = token;
  }

  return {
    url,
    options: { ...options, headers },
  };
});

// 响应拦截器
request.interceptors.response.use(async (response) => {
  try {
    const text = await response.clone().text();
    
    // 使用parseJSON处理响应数据
    const data = parseJSON(text);
    
    // 处理所有可能包含id的数据
    const processData = (obj: any): any => {
      if (!obj) return obj;

      if (Array.isArray(obj)) {
        return obj.map(item => processData(item));
      }

      if (typeof obj === 'object') {
        const processed = { ...obj };
        for (const key in processed) {
          //key字段包含id，则转换为string
          if (key.includes('id') && typeof processed[key] === 'number') {
            processed[key] = String(processed[key]);
          } else if (typeof processed[key] === 'object') {
            processed[key] = processData(processed[key]);
          }
        }
        return processed;
      }

      return obj;
    };

    // 处理整个响应数据
    const processedData = processData(data);

    if (processedData.code === 401) {
      // 未登录或 token 失效
      message.error('请先登录');
      window.location.href = '/api/user/login';
      return response;
    }

    // 创建新的响应对象
    const newResponse = new Response(stringifyJSON(processedData), {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
    });


    return newResponse;
  } catch (error) {
    console.error('Response parsing error:', error);
    return response;
  }
});

export default request; 