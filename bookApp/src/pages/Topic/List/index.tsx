import { PlusOutlined, EditOutlined, DeleteOutlined, ImportOutlined } from '@ant-design/icons';
import type { ProColumns, ActionType } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tooltip, Tag } from 'antd';
import { useState, useRef } from 'react';
import request from '@/utils/request';
import type { TopicItem } from './data.d';
import TopicForm from './components/TopicForm';
import TopicImportModal from './components/TopicImportModal';

const TopicList: React.FC = () => {
  const [createModalVisible, handleModalVisible] = useState<boolean>(false);
  const [importModalVisible, setImportModalVisible] = useState<boolean>(false);
  const [currentRow, setCurrentRow] = useState<TopicItem>();
  const actionRef = useRef<ActionType>();

  const handleAdd = async (fields: TopicItem) => {
    const hide = message.loading('正在添加');
    try {
      await request('/api/topic', {
        method: 'POST',
        data: fields,
      });
      hide();
      message.success('添加成功');
      return true;
    } catch (error) {
      hide();
      message.error('添加失败，请重试');
      return false;
    }
  };

  const handleUpdate = async (fields: TopicItem) => {
    const hide = message.loading('正在更新');
    try {
      await request('/api/topic', {
        method: 'PUT',
        data: fields,
      });
      hide();
      message.success('更新成功');
      return true;
    } catch (error) {
      hide();
      message.error('更新失败，请重试');
      return false;
    }
  };

  const handleRemove = async (id: string) => {
    const hide = message.loading('正在删除');
    try {
      await request(`/api/topic/${id}`, {
        method: 'DELETE',
      });
      hide();
      message.success('删除成功');
      return true;
    } catch (error) {
      hide();
      message.error('删除失败，请重试');
      return false;
    }
  };

  // 处理数据转换为树形结构
  const convertToTreeData = (data: TopicItem[]): TopicItem[] => {
    const map = new Map<string, TopicItem>();
    const result: TopicItem[] = [];

    // 先将所有节点放入map
    data.forEach(item => {
      if (item.id) {
        map.set(item.id, { ...item, children: [] });
      }
    });

    // 构建树形结构
    data.forEach(item => {
      if (item.id) {
        const node = map.get(item.id);
        if (item.parentId && map.has(item.parentId)) {
          const parent = map.get(item.parentId);
          if (parent && parent.children) {
            parent.children.push(node!);
          }
        } else {
          result.push(node!);
        }
      }
    });

    // 清理空的children数组
    const cleanEmptyChildren = (nodes: TopicItem[]): TopicItem[] => {
      return nodes.map(node => {
        if (node.children && node.children.length === 0) {
          const { children, ...rest } = node;
          return rest;
        }
        if (node.children) {
          return {
            ...node,
            children: cleanEmptyChildren(node.children),
          };
        }
        return node;
      });
    };

    return cleanEmptyChildren(result);
  };

  // 获取层级对应的Tag颜色
  const getLevelTagColor = (level: number) => {
    const colors = ['blue', 'green', 'gold', 'orange', 'volcano', 'purple'];
    return level === 0 ? 'blue' : colors[level % colors.length];
  };

  const columns: ProColumns<TopicItem>[] = [
    {
      title: '标签名称',
      dataIndex: 'name',
      width: 300,
      ellipsis: true,
      fixed: 'left',
      render: (_, record) => (
        <span style={{ 
          paddingLeft: `${(record.level || 0) * 20}px`,
          color: record.children ? '#1890ff' : 'inherit',
          fontWeight: record.children ? 500 : 'normal'
        }}>
          {record.name}
        </span>
      ),
    },
    {
      title: '层级',
      dataIndex: 'level',
      width: 100,
      search: true,
      render: (_, record) => (
        <Tag color={getLevelTagColor(record.level || 0)} style={{ minWidth: '60px', textAlign: 'center' }}>
          {record.level === 0 ? '根节点' : `第${record.level}级`}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 150,
      valueType: 'dateTime',
      sorter: true,
      search: false,
    },
    {
      title: '更新时间',
      dataIndex: 'modifiedTime',
      width: 150,
      valueType: 'dateTime',
      sorter: true,
      search: false,
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      width: 180,
      fixed: 'right',
      render: (_, record) => [
        <Tooltip title="编辑" key="edit-tooltip">
          <Button
            key="edit"
            type="link"
            icon={<EditOutlined />}
            onClick={() => {
              setCurrentRow(record);
              handleModalVisible(true);
            }}
          />
        </Tooltip>,
        <Popconfirm
          key="delete"
          title="确定要删除这个标签吗？"
          onConfirm={async () => {
            if (await handleRemove(record.id!)) {
              actionRef.current?.reload();
            }
          }}
        >
          <Tooltip title="删除" key="delete-tooltip">
            <Button icon={<DeleteOutlined />} type="link" danger />
          </Tooltip>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<TopicItem>
        headerTitle="标签列表"
        rowKey="id"
        scroll={{ x: 1500 }}
        search={{
          labelWidth: 120,
          defaultCollapsed: false,
        }}
        pagination={false}
        expandable={{
          defaultExpandAllRows: true,
        }}
        toolBarRender={() => [
          <Button
            key="import"
            onClick={() => setImportModalVisible(true)}
          >
            <ImportOutlined /> 批量导入
          </Button>,
          <Button
            type="primary"
            key="primary"
            onClick={() => {
              setCurrentRow(undefined);
              handleModalVisible(true);
            }}
          >
            <PlusOutlined /> 新增
          </Button>,
        ]}
        request={async (params) => {
          const { current, pageSize, ...restParams } = params;
          const response = await request('/api/topic/list', {
            method: 'GET',
            params: restParams,
          });
          
          const treeData = convertToTreeData(response.data || []);
          
          return {
            data: treeData,
            success: response.code === 0,
          };
        }}
        columns={columns}
        actionRef={actionRef}
      />
      <TopicForm
        visible={createModalVisible}
        onVisibleChange={(visible: boolean) => {
          handleModalVisible(visible);
          if (!visible) {
            setCurrentRow(undefined);
          }
        }}
        onFinish={async (values: TopicItem) => {
          let success;
          if (currentRow?.id) {
            success = await handleUpdate({ ...currentRow, ...values });
          } else {
            success = await handleAdd(values);
          }
          if (success) {
            handleModalVisible(false);
            setCurrentRow(undefined);
            if (actionRef.current) {
              actionRef.current.reload();
            }
          }
          return success;
        }}
        values={currentRow}
      />
      <TopicImportModal
        visible={importModalVisible}
        onVisibleChange={setImportModalVisible}
        onSuccess={() => {
          actionRef.current?.reload();
        }}
      />
    </PageContainer>
  );
};

export default TopicList; 