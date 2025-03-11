import dotenv from 'dotenv';
import path from 'path';

const envPath = path.resolve(__dirname, '../.env.development');
console.log('envPath:', envPath);

dotenv.config({ path: envPath });

console.log('VITE_DEEPSEEK_API_KEY:', process.env.VITE_DEEPSEEK_API_KEY);

/**
 * @name 代理的配置
 * @see 在生产环境 代理是无法生效的，所以这里没有生产环境的配置
 * -------------------------------
 * The agent cannot take effect in the production environment
 * so there is no configuration of the production environment
 * For details, please see
 * https://pro.ant.design/docs/deploy
 *
 * @doc https://umijs.org/docs/guides/proxy
 */
export default {
  dev: {
    '/api/': {
      // target: 'http://192.168.50.88:9090',
      target: 'http://localhost:9090',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
    '/deepseek/': {
      target: 'https://api.deepseek.com/v1',
      changeOrigin: true,
      pathRewrite: { '^/deepseek': '' },
      headers: {
        'Authorization': `Bearer ${process.env.VITE_DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json',
      },
    },
    '/qwen/': {
      target: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      changeOrigin: true,
      pathRewrite: { '^/qwen': '' },
      headers: {
        'Authorization': `Bearer ${process.env.DASHSCOPE_API_KEY}`,
        'Content-Type': 'application/json',
      },
    },
    '/ai/': {
      target: 'https://api.deepseek.com/v1/chat',
      changeOrigin: true,
      pathRewrite: { '^/ai': '' },
      headers: {
        'Authorization': `Bearer ${process.env.VITE_DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json',
      },
    },
    '/api2/': {
      // target: 'http://192.168.50.88:9000/',
      target: 'http://192.168.50.25:9079/i/',
      // target: 'http://localhost:9000/',
      changeOrigin: true,
      secure: false,
      pathRewrite: { '^/api2/': '' },
    },
    '/api3/': {
      // target: 'http://192.168.50.88:9000/',
      target: 'http://192.168.50.24:19000/',
      // target: 'http://localhost:9000/',
      changeOrigin: true,
      secure: false,
      pathRewrite: { '^/api3/': '' },
    },
    '/es/': {
      // target: 'http://192.168.50.88:9200/',
      target: 'http://192.168.50.25:9200/',
      changeOrigin: true,
      secure: false,
      pathRewrite: { '^/es/': '' },
    },
  },
  test: {
    '/api/': {
      target: 'http://192.168.50.88:9090',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
  },
  pre: {
    '/api/': {
      target: 'http://192.168.50.88:9090',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
  },
};
