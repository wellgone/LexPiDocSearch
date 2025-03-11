import { XRequest } from '@ant-design/x';

export interface ModelConfig {
  name: string;
  label: string;
  baseURL: string;
  apiKey: string;
}

// 模型配置
export const models: ModelConfig[] = [
  {
    name: 'deepseek-chat',
    label: 'DeepSeek-V3',
    baseURL: '/deepseek/chat',
    apiKey: process.env.VITE_DEEPSEEK_API_KEY || '',
  },
  {
    name: 'deepseek-reasoner',
    label: 'DeepSeek-R1',
    baseURL: '/deepseek/chat',
    apiKey: process.env.VITE_DEEPSEEK_API_KEY || '',

  },
  {
    name: 'qwen',
    label: '通义千问',
    baseURL: '/qwen/chat',
    apiKey: process.env.DASHSCOPE_API_KEY || '',
  },
];

// 创建模型请求实例
export const createModelRequest = (model: ModelConfig) => {
  return XRequest({
    baseURL: model.baseURL,
    dangerouslyApiKey: model.apiKey,
    model: model.name,
  });
};
