import { useState } from 'react';
import type { LoginFormProps } from '@ant-design/pro-components';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import { message } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import request from '@/utils/request';
import styles from './index.less';

// 定义登录参数接口
interface LoginParams {
  userAccount: string;
  userPassword: string;
}

// 定义登录响应接口
interface LoginResponse {
  code: number;
  data: {
    token: string;
    user: {
      userId: number;
      userAccount: string;
      userName: string;
      userEmail: string;
      userRole: string;
    };
  };
  message: string;
}

// 定义初始状态接口
interface InitialState {
  currentUser?: LoginResponse['data']['user'];
  settings?: Record<string, unknown>;
}

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const { setInitialState } = useModel('@@initialState');

  // 处理登录提交
  const handleSubmit = async (values: LoginParams) => {
    try {
      setLoading(true);
      
      // 使用request实例发送登录请求
      const result = await request<LoginResponse>('/api/user/login', {
        method: 'POST',
        data: values,
      });
      
      if (result.code === 0 && result.data.token) {
        // 保存 token 到 localStorage
        localStorage.setItem('token', result.data.token);
        
        message.success(result.message || '登录成功！');
        
        // 更新全局状态
        await setInitialState((s: InitialState) => ({
          ...s,
          currentUser: result.data.user,
        }));
        
        // 延迟跳转，确保token已经被正确设置
        setTimeout(() => {
          const urlParams = new URL(window.location.href);
          const redirect = urlParams.searchParams.get('redirect');
          window.location.href = redirect || '/welcome';
        }, 100);
        
        return;
      } else {
        message.error(result.message || '登录失败，请重试！');
      }
    } catch (error: any) {
      message.error(error.message || '登录失败，请重试！');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.content}>
        <LoginForm
          // logo={<img alt="logo" src="/logo.png" />}
          title="律π"
          subTitle="知识管理"
          initialValues={{
            autoLogin: true,
          }}

          onFinish={async (values: LoginFormProps) => {
            await handleSubmit(values as unknown as LoginParams);
          }}
          submitter={{
            searchConfig: {
              submitText: '登录',
            },
            submitButtonProps: {
              loading: loading,
              size: 'large',
              style: {
                width: '100%',
              },
            },
          }}
        >
          <ProFormText
            name="userAccount"
            fieldProps={{
              size: 'large',
              prefix: <UserOutlined className={styles.prefixIcon} />,
            }}
            placeholder="用户名"
            rules={[
              {
                required: true,
                message: '请输入用户名!',
              },
              {
                min: 4,
                max: 20,
                message: '用户名长度必须在4-20位之间！',
              },
              {
                pattern: /^[a-zA-Z0-9_]+$/,
                message: '用户名只能包含字母、数字和下划线！',
              },
            ]}
          />
          <ProFormText.Password
            name="userPassword"
            fieldProps={{
              size: 'large',
              prefix: <LockOutlined className={styles.prefixIcon} />,
            }}
            placeholder="密码"
            rules={[
              {
                required: true,
                message: '请输入密码！',
              },
              {
                min: 8,
                max: 20,
                message: '密码长度必须在8-20位之间！',
              },
            ]}
          />
        </LoginForm>
      </div>
    </div>
  );
};

export default Login;
