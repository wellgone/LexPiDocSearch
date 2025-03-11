import React, { useState, useEffect } from 'react';
import { Descriptions, Button, Modal, Form, Input, message, Menu, Avatar } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import { getCurrentUser, UserInfo } from '@/services/auth';
import request from '@/utils/request';
import { ProCard } from '@ant-design/pro-components';

const Profile: React.FC = () => {
  const [form] = Form.useForm();
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);
  const [userInfo, setUserInfo] = useState<UserInfo>();
  const [activeTab, setActiveTab] = useState('basic');

  const fetchUserInfo = async () => {
    try {
      const data = await getCurrentUser();
      setUserInfo(data);
    } catch (error) {
      message.error('获取用户信息失败');
    }
  };

  useEffect(() => {
    fetchUserInfo();
  }, []);

  const handleChangePassword = async (values: { oldPassword: string; newPassword: string }) => {
    try {
      await request('/api/user/change-password', {
        method: 'POST',
        data: values,
      });
      message.success('密码修改成功');
      setPasswordModalVisible(false);
      form.resetFields();
    } catch (error) {
      message.error('密码修改失败');
    }
  };

  const BasicInfo = () => (
    <div className="p-8">
      <div className="flex items-center mb-10 bg-white rounded-lg p-6 shadow-sm">
        <Avatar 
          size={100} 
          icon={<UserOutlined />} 
          className="mr-8 shadow-md"
          style={{ 
            backgroundColor: '#1890ff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        />
        <div>
          <h1 className="text-3xl font-bold mb-3 text-gray-800">{userInfo?.userName}</h1>
          <p className="text-gray-500 text-lg flex items-center">
            <span className="inline-flex items-center justify-center bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-medium mr-3">
              {userInfo?.userRole === 'admin' ? '管理员' : '普通用户'}
            </span>
            <span className="flex items-center text-gray-600">
              <MailOutlined className="mr-1" /> {userInfo?.userEmail}
            </span>
          </p>
        </div>
      </div>
      <ProCard 
        title={<span className="text-lg font-medium">个人信息</span>} 
        className="shadow-sm"
        headerBordered
      >
        <Descriptions 
          column={2} 
          className="py-4"
          labelStyle={{ 
            color: '#666',
            fontWeight: 500,
            padding: '12px 24px 12px 0',
          }}
          contentStyle={{
            color: '#333',
            padding: '12px 24px 12px 0',
          }}
        >
          <Descriptions.Item label="用户名">
            {userInfo?.userName}
          </Descriptions.Item>
          <Descriptions.Item label="账号">
            {userInfo?.userAccount}
          </Descriptions.Item>
          <Descriptions.Item label="邮箱">
            {userInfo?.userEmail}
          </Descriptions.Item>
          <Descriptions.Item label="角色">
            {userInfo?.userRole === 'admin' ? '管理员' : '普通用户'}
          </Descriptions.Item>
          <Descriptions.Item label="今日登录次数">
            <span className="text-blue-600 font-medium">{userInfo?.userLoginNum}</span>
          </Descriptions.Item>
          <Descriptions.Item label="注册时间">
            {userInfo?.createTime}
          </Descriptions.Item>
        </Descriptions>
      </ProCard>
    </div>
  );

  const SecuritySettings = () => (
    <div className="p-8">
      <ProCard 
        title={<span className="text-lg font-medium">安全设置</span>}
        className="shadow-sm"
        headerBordered
      >
        <div className="py-6 border-b border-gray-100">
          <div className="flex justify-between items-center mb-2">
            <h3 className="text-base font-medium text-gray-800 flex items-center">
              <LockOutlined className="mr-2 text-blue-500" />
              账户密码
            </h3>
            <Button 
              type="primary" 
              onClick={() => setPasswordModalVisible(true)}
              size="large"
              className="shadow-sm"
            >
              修改密码
            </Button>
          </div>
          <p className="text-gray-500 text-sm leading-relaxed max-w-lg">
            建议您定期更改密码，设置一个包含字母、数字和特殊字符的强密码
          </p>
        </div>
        <div className="flex justify-between items-center py-6">
          <div className="max-w-lg">
            <h3 className="text-base font-medium mb-2 text-gray-800 flex items-center">
              <MailOutlined className="mr-2 text-blue-500" />
              绑定邮箱
            </h3>
            <p className="text-gray-500 text-sm leading-relaxed">
              已绑定邮箱：
              <span className="text-gray-800 font-medium ml-1">{userInfo?.userEmail}</span>
            </p>
          </div>
        </div>
      </ProCard>
    </div>
  );

  return (
    <div className="bg-gray-50 min-h-screen">
      <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
        <ProCard 
          split="vertical" 
          bordered 
          className="shadow-lg"
          style={{ minHeight: '600px' }}
        >
          <ProCard 
            colSpan="280px" 
            className="!px-0"
            style={{ 
              borderRight: '1px solid #f0f0f0',
            }}
          >
            <Menu
              mode="inline"
              selectedKeys={[activeTab]}
              onClick={({ key }) => setActiveTab(key)}
              items={[
                {
                  key: 'basic',
                  icon: <UserOutlined />,
                  label: '基本信息',
                },
                {
                  key: 'security',
                  icon: <LockOutlined />,
                  label: '安全设置',
                },
              ]}
              style={{
                height: '100%',
                borderRight: 0,
                fontSize: '15px',
              }}
            />
          </ProCard>
          <ProCard className="!px-0">
            {activeTab === 'basic' ? <BasicInfo /> : <SecuritySettings />}
          </ProCard>
        </ProCard>
      </div>

      <Modal
        title={
          <div className="flex items-center text-lg font-medium pb-2">
            <LockOutlined className="mr-2 text-blue-500" />
            修改密码
          </div>
        }
        open={passwordModalVisible}
        onCancel={() => setPasswordModalVisible(false)}
        footer={null}
        width={480}
        className="rounded-lg"
      >
        <Form
          form={form}
          onFinish={handleChangePassword}
          layout="vertical"
          className="pt-4"
        >
          <Form.Item
            name="oldPassword"
            label="旧密码"
            rules={[
              { required: true, message: '请输入旧密码' },
              { min: 8, max: 20, message: '密码长度必须在8-20位之间' },
            ]}
          >
            <Input.Password 
              placeholder="请输入旧密码" 
              size="large"
              className="rounded"
            />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, max: 20, message: '密码长度必须在8-20位之间' },
            ]}
          >
            <Input.Password 
              placeholder="请输入新密码" 
              size="large"
              className="rounded"
            />
          </Form.Item>
          <Form.Item className="mb-0 text-right">
            <Button 
              type="default" 
              onClick={() => setPasswordModalVisible(false)}
              className="mr-3"
              size="large"
            >
              取消
            </Button>
            <Button 
              type="primary" 
              htmlType="submit"
              size="large"
            >
              确认修改
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Profile; 