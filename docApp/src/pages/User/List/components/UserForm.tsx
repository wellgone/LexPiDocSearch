import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import type { UserType, UserFormValues } from '../data.d';

interface UserFormProps {
  modalVisible: boolean;
  onCancel: () => void;
  onSubmit: (values: UserFormValues) => Promise<void>;
  values?: UserType;
}

const UserForm: React.FC<UserFormProps> = ({ modalVisible, onCancel, onSubmit, values }) => {
  const [form] = Form.useForm();
  const isEdit = !!values;

  useEffect(() => {
    if (values) {
      form.setFieldsValue({
        userAccount: values.userAccount,
        userName: values.userName,
        userEmail: values.userEmail,
        userRole: values.userRole,
        userState: values.userState,
      });
    } else {
      form.resetFields();
    }
  }, [values, form]);

  const handleSubmit = async () => {
    try {
      const formValues = await form.validateFields();
      await onSubmit(formValues);
      form.resetFields();
    } catch (error) {
      message.error('表单验证失败');
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑用户' : '新增用户'}
      open={modalVisible}
      onCancel={onCancel}
      onOk={handleSubmit}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          userRole: 'user',
          userState: 0,
        }}
      >
        <Form.Item
          name="userAccount"
          label="用户账号"
          rules={[{ required: true, message: '请输入用户账号' }]}
        >
          <Input placeholder="请输入用户账号" disabled={isEdit} />
        </Form.Item>
        {!isEdit && (
          <Form.Item
            name="userPassword"
            label="用户密码"
            rules={[{ required: true, message: '请输入用户密码' }]}
          >
            <Input.Password placeholder="请输入用户密码" />
          </Form.Item>
        )}
        <Form.Item
          name="userName"
          label="用户名"
          rules={[{ required: true, message: '请输入用户名' }]}
        >
          <Input placeholder="请输入用户名" />
        </Form.Item>
        <Form.Item
          name="userEmail"
          label="邮箱"
          rules={[
            { required: true, message: '请输入邮箱' },
            { type: 'email', message: '请输入正确的邮箱格式' },
          ]}
        >
          <Input placeholder="请输入邮箱" />
        </Form.Item>
        <Form.Item
          name="userRole"
          label="用户角色"
          rules={[{ required: true, message: '请选择用户角色' }]}
        >
          <Select>
            <Select.Option value="admin">管理员</Select.Option>
            <Select.Option value="user">普通用户</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item
          name="userState"
          label="用户状态"
          rules={[{ required: true, message: '请选择用户状态' }]}
        >
          <Select>
            <Select.Option value={0}>启用</Select.Option>
            <Select.Option value={1}>禁用</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default UserForm; 