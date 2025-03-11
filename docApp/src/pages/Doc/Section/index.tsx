import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ProColumns, ActionType } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm } from 'antd';
import { useState, useRef } from 'react';
import request from '@/utils/request';
import SectionForm from './components/SectionForm';
import type { DocSection } from './data.d';

const DocSectionList: React.FC = () => {
  const [createModalVisible, handleModalVisible] = useState<boolean>(false);
  const [currentRow, setCurrentRow] = useState<DocSection>();
  const actionRef = useRef<ActionType>();

  const handleAdd = async (fields: DocSection) => {
    const hide = message.loading('正在添加');
    try {
      await request('/api/doc-section/add', {
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

  const handleUpdate = async (fields: DocSection) => {
    const hide = message.loading('正在更新');
    try {
      await request('/api/doc-section/update', {
        method: 'PUT',
        data: { ...fields, id: currentRow?.id },
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
      await request(`/api/doc-section/delete/${id}`, {
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

  const columns: ProColumns<DocSection>[] = [
    {
        title: 'ID',
        dataIndex: 'id',
        width: 80,
      },
    {
      title: '文档ID',
      dataIndex: 'docId',
      width: 80,
    },
    {
      title: '文档名称',
      dataIndex: 'title',
      width: 150,
      search: true,
    },
    {
      title: '页码',
      dataIndex: 'pageNum',
      width: 80,
      search: true,
      valueType: 'digit',
    },
    {
      title: '内容',
      dataIndex: 'content',
      width: 400,
      ellipsis: true,
      search: true,
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
      width: 120,
      fixed: 'right',
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          icon={<EditOutlined />}
          onClick={() => {
            setCurrentRow(record);
            handleModalVisible(true);
          }}
        >
        </Button>,
        <Popconfirm
          key="delete"
          title="确定要删除该记录(对应索引将同步删除)？"
          onConfirm={async () => {
            if (await handleRemove(record.id!)) {
              actionRef.current?.reload();
            }
          }}
        >
          <Button type="link" danger icon={<DeleteOutlined />}>
          </Button>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<DocSection>
        headerTitle="图书章节列表"
        rowKey="id"
        scroll={{ x: 1300 }}
        search={{
          labelWidth: 120,
          defaultCollapsed: false,
        }}
        toolBarRender={() => [
        ]}
        request={async (params) => {
          const { current, pageSize, ...restParams } = params;
          const response = await request('/api/doc-section/list', {
            method: 'GET',
            params: {
              ...restParams,
              current: current || 1,
              size: pageSize || 10,
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
      />
      <SectionForm
        visible={createModalVisible}
        onVisibleChange={(visible: boolean) => {
          handleModalVisible(visible);
          if (!visible) {
            setCurrentRow(undefined);
          }
        }}
        onFinish={async (values: DocSection) => {
          let success;
          if (currentRow?.id) {
            success = await handleUpdate({
              ...values,
              id: currentRow.id,
              createTime: currentRow.createTime,
              modifiedTime: currentRow.modifiedTime,
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
      />
    </PageContainer>
  );
};

export default DocSectionList; 