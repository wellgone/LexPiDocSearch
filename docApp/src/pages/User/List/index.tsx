import React, { useRef, useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import request from '@/utils/request';
import UserForm from './components/UserForm';
import type { UserType, UserFormValues } from './data.d.ts';

const UserList: React.FC = () => {
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [editModalVisible, setEditModalVisible] = useState<boolean>(false);
  const [currentUser, setCurrentUser] = useState<UserType>();
  const actionRef = useRef<ActionType>();

  // 获取用户列表
  const getUsers = async (params: any) => {
    try {
      const response = await request('/api/user/list', {
        method: 'GET',
        params: {
          current: params.current,
          pageSize: params.pageSize,
        },
      });

      // 检查响应数据结构
      if (response.code === 0 && response.data) {
        console.log(response.data);
        // 确保 userId 作为字符串处理
        const users = response.data.records.map((user: any) => ({
          ...user,
          userId: user.userId ? user.userId.toString() : '',
          createTime: user.createTime ? new Date(user.createTime).toLocaleString() : '',
          modifiedTime: user.modifiedTime ? new Date(user.modifiedTime).toLocaleString() : '',
        }));

        return {
          data: users,
          success: true,
          total: response.data.total || 0,
        };
      }

      message.error(response.message || '获取用户列表失败');
      return {
        data: [],
        success: false,
        total: 0,
      };
    } catch (error) {
      message.error('获取用户列表失败');
      return {
        data: [],
        success: false,
        total: 0,
      };
    }
  };

  // 删除用户
  const handleDelete = async (id: string) => {
    try {
      const response = await request(`/user/delete/${id}`, {
        method: 'DELETE',
      });
      
      if (response.code === 0) {
        message.success('删除成功');
        actionRef.current?.reload();
      } else {
        message.error(response.message || '删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 表格列定义
  const columns: ProColumns<UserType>[] = [
    {
      title: '用户ID',
      dataIndex: 'userId',
      hideInSearch: true,
      copyable: true,
      ellipsis: true,
    },
    {
      title: '用户账号',
      dataIndex: 'userAccount',
    },
    {
      title: '用户名',
      dataIndex: 'userName',
    },
    {
      title: '邮箱',
      dataIndex: 'userEmail',
      ellipsis: true,
    },
    {
      title: '角色',
      dataIndex: 'userRole',
      valueEnum: {
        admin: { text: '管理员', status: 'success' },
        user: { text: '普通用户', status: 'default' },
      },
      filters: true,
      onFilter: true,
    },
    {
      title: '状态',
      dataIndex: 'userState',
      filters: true,
      onFilter: true,
      valueEnum: {
        0: { text: '启用', status: 'success' },
        1: { text: '禁用', status: 'error' },
      },
    },
    {
      title: '今日登录次数',
      dataIndex: 'userLoginNum',
      hideInSearch: true,
      sorter: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      hideInSearch: true,
      sorter: true,
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => [
        <a
          key="edit"
          onClick={() => {
            setCurrentUser(record);
            setEditModalVisible(true);
          }}
        >
          编辑
        </a>,
        <a
          key="delete"
          onClick={() => {
            Modal.confirm({
              title: '确认删除',
              content: '确定要删除该用户吗？',
              onOk: () => handleDelete(record.userId),
            });
          }}
        >
          删除
        </a>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<UserType>
        headerTitle="用户列表"
        actionRef={actionRef}
        rowKey="userId"
        search={{
          labelWidth: 120,
        }}
        toolBarRender={() => [
          <Button
            type="primary"
            key="primary"
            onClick={() => {
              setCreateModalVisible(true);
            }}
          >
            <PlusOutlined /> 新增
          </Button>,
        ]}
        request={getUsers}
        columns={columns}
        pagination={{
          defaultPageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
        }}
      />
      <UserForm
        modalVisible={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
        }}
        onSubmit={async (values: UserFormValues) => {
          try {
            const response = await request('/user/register', {
              method: 'POST',
              data: values,
            });

            if (response.code === 0) {
              message.success('添加成功');
              setCreateModalVisible(false);
              actionRef.current?.reload();
            } else {
              message.error(response.message || '添加失败');
            }
          } catch (error) {
            message.error('添加失败');
          }
        }}
      />
      <UserForm
        modalVisible={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false);
        }}
        onSubmit={async (values: UserFormValues) => {
          try {
            const response = await request('/user/update', {
              method: 'PUT',
              data: {
                ...values,
                userId: currentUser?.userId,
              },
            });

            if (response.code === 0) {
              message.success('更新成功');
              setEditModalVisible(false);
              actionRef.current?.reload();
            } else {
              message.error(response.message || '更新失败');
            }
          } catch (error) {
            message.error('更新失败');
          }
        }}
        values={currentUser}
      />
    </PageContainer>
  );
};

export default UserList; 