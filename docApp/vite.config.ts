import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig(({ mode }) => {
  // 加载env文件
  const env = loadEnv(mode, process.cwd(), '');
  
  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    // 定义环境变量
    define: {
      'import.meta.env.VITE_ES_URL': JSON.stringify(env.VITE_ES_URL || 'http://localhost:9200'),
      'import.meta.env.VITE_ES_USERNAME': JSON.stringify(env.VITE_ES_USERNAME || ''),
      'import.meta.env.VITE_ES_PASSWORD': JSON.stringify(env.VITE_ES_PASSWORD || ''),
      'import.meta.env.VITE_API_URL': JSON.stringify(env.VITE_API_URL || 'http://localhost:9090'),
    },
    server: {
      host: '0.0.0.0',
      port: 8000,
      proxy: {
        '/api': {
          target: env.VITE_API_URL || 'http://localhost:9090',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
        '/es': {
          target: env.VITE_ES_URL || 'http://localhost:9200',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/es/, ''),
        },
      },
    },
  };
}); 