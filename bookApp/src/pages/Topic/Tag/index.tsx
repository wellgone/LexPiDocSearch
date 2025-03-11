import { PlusOutlined, EditOutlined, DeleteOutlined, ImportOutlined } from '@ant-design/icons';
import type { ProColumns, ActionType } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tooltip } from 'antd';
import { useState, useRef, useEffect } from 'react';
import request from '@/utils/request';
import type { TagItem } from './data.d';
import TagForm from './components/TagForm';
import TagImportModal from './components/TagImportModal';

const TagList: React.FC = () => {
  const [createModalVisible, handleModalVisible] = useState<boolean>(false);
  const [importModalVisible, setImportModalVisible] = useState<boolean>(false);
  const [currentRow, setCurrentRow] = useState<TagItem>();
  const [tagTypes, setTagTypes] = useState<any[]>([]);
  const actionRef = useRef<ActionType>();

  // 获取标签类型
  const fetchTagTypes = async () => {
    try {
      const response = await request('/api/topic/types');
      if (response.code === 0) {
        console.log('获取到标签类型：', response.data);
        setTagTypes(response.data);
      }
    } catch (error) {
      message.error('获取标签类型失败');
    }
  };

  useEffect(() => {
    fetchTagTypes();
  }, []);

  // 监听currentRow变化
  useEffect(() => {
    console.log('当前选中的行数据：', currentRow);
  }, [currentRow]);

  const handleAdd = async (fields: TagItem) => {
    const hide = message.loading('正在添加');
    try {
      await request('/api/topic', {
        method: 'POST',
        data: {
          ...fields,
          level: fields.level || tagTypes.find(t => t.value === fields.type)?.level || 100,
          type: fields.type || '通用标签',
          parentId: 0,
        },
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

  const handleUpdate = async (fields: TagItem) => {
    console.log('更新数据：', fields);
    const hide = message.loading('正在更新');
    try {
      await request('/api/topic', {
        method: 'PUT',
        data: {
          ...currentRow,
          ...fields,
          parentId: null,
        },
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

  const handleRemove = async (id: number) => {
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

  const columns: ProColumns<TagItem>[] = [
    {
      title: '标签名称',
      dataIndex: 'name',
      width: 200,
      ellipsis: true,
      fixed: 'left',
    },
    {
      title: '标签类型',
      dataIndex: 'type',
      width: 150,
      valueType: 'select',
      valueEnum: tagTypes.reduce((acc, curr) => ({
        ...acc,
        [curr.value]: { text: curr.label },
      }), {}),
      fieldProps: {
        options: tagTypes,
      },
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
              console.log('编辑按钮点击，当前行数据：', record);
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
      <ProTable<TagItem>
        headerTitle="标签管理"
        rowKey="id"
        scroll={{ x: 1200 }}
        search={{
          labelWidth: 120,
          defaultCollapsed: false,
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
        request={async (params, sorter) => {
          // 处理排序参数
          const sortField = Object.keys(sorter)[0] || 'createTime';
          const sortOrder = sorter[sortField] === 'ascend' ? 'asc' : 'desc';

          // 处理查询参数
          const { current, pageSize, name, type } = params;

          // 将type转换为对应的level
          const level = type ? tagTypes.find(t => t.value === type)?.level : undefined;

          const response = await request('/api/topic/tags', {
            method: 'GET',
            params: {
              current,
              pageSize,
              name: name?.trim(),
              level,
              sortField: sortField?.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`), // 驼峰转下划线
              sortOrder,
            },
          });

          return {
            data: response.data.records,
            success: response.code === 0,
            total: response.data.total,
          };
        }}
        columns={columns}
        actionRef={actionRef}
        pagination={{
          pageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
        }}
      />
      <TagForm
        visible={createModalVisible}
        onVisibleChange={(visible: boolean) => {
          handleModalVisible(visible);
          if (!visible) {
            setCurrentRow(undefined);
          }
        }}
        onFinish={async (values: TagItem) => {
          console.log('表单提交的数据：', values);
          let success;
          if (currentRow?.id) {
            success = await handleUpdate({
              ...values,
              id: currentRow.id,
            });
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
        tagTypes={tagTypes}
      />
      <TagImportModal
        visible={importModalVisible}
        onVisibleChange={setImportModalVisible}
        onSuccess={() => {
          actionRef.current?.reload();
        }}
        tagTypes={tagTypes}
      />
    </PageContainer>
  );
};

export default TagList; 