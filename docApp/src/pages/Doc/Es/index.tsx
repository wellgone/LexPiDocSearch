import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useRef } from 'react';
import { request } from '@umijs/max';
import { Button, message, Popconfirm } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';

interface DocSectionDocument {
  id: string;
  docId: string;
  docTitle: string;
  author: string;
  publisher: string;
  publicationYear: string;
  picPath: string;
  filePath: string;
  isbn: string;
  pageNum: number;
  sectionText: string;
  timestamp: string;
  version: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  size: number;
  number: number;
}

interface BaseResponse<T> {
  code: number;
  data: T;
  message: string;
}

export default function DocEsList() {
  const actionRef = useRef<ActionType>();

  // 删除章节
  const handleDelete = async (record: DocSectionDocument) => {
    const hide = message.loading('正在删除...');
    try {
      const response = await request<BaseResponse<string>>(`/api/es/docs/${record.id}`, {
        method: 'DELETE',
      });
      hide();
      if (response.code === 0) {
        message.success(response.message || '删除成功');
        actionRef.current?.reload();
      } else {
        message.error(response.message || '删除失败');
      }
    } catch (error) {
      hide();
      message.error('删除失败，请重试');
    }
  };

  const columns: ProColumns<DocSectionDocument>[] = [
    {
      title: '索引ID',
      dataIndex: 'id',
      search: true,
      width: 90,
    },
    {
      title: '文档ID',
      dataIndex: 'docId',
      search: true,
      width: 60,
    },
    {
      title: '文档名称',
      dataIndex: 'docTitle',
      search: true,
    },
    {
      title: 'OPAC系列',
      dataIndex: 'opacSeries',
      search: true,
    },
    {
      title: '图书系列',
      dataIndex: 'series',
      search: true,
    },
    {
      title: '出版年份',
      dataIndex: 'publicationYear',
      search: true,
    },
    {
      title: '作者',
      dataIndex: 'author',
      search: true,
    },
    {
      title: '出版社',
      dataIndex: 'publisher',
      search: false,
    },
    {
      title: 'ISBN',
      dataIndex: 'isbn',
      search: true,
    },
    {
      title: '标签',
      dataIndex: 'tags',
      search: true,
    },
    {
      title: '主题系列',
      dataIndex: 'topicSeries',
      search: true,
    },
    {
      title: '页码',
      dataIndex: 'pageNum',
      search: true,
    },
    {
      title: '章节内容',
      dataIndex: 'sectionText',
      search: true,
      ellipsis: true,
    },
    {
      title: '更新时间',
      dataIndex: 'timestamp',
      valueType: 'dateTime',
      search: false,
    },
    {
      title: '操作',
      valueType: 'option',
      key: 'option',
      render: (_, record) => [
        <Popconfirm
          key="delete"
          title="确认删除"
          description="确定要删除该索引吗？"
          onConfirm={() => handleDelete(record)}
        >
          <Button type="link" danger icon={<DeleteOutlined />}>
            删除
          </Button>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<DocSectionDocument>
        columns={columns}
        actionRef={actionRef}
        cardBordered
        request={async (params = {}) => {
          const { current, pageSize, docTitle, sectionText, author, publisher, isbn, pageNum, docId } = params;
          try {
            const response = await request<BaseResponse<PageResponse<DocSectionDocument>>>('/api/es/docs/search', {
              method: 'GET',
              params: {
                page: (current || 1) - 1,
                size: pageSize,
                keyword: sectionText,
                docTitle,
                docId,
                pageNum,
                author,
                publisher,
                isbn,
              },
            });
            
            if (response.code === 0 && response.data) {
              return {
                data: response.data.content,
                success: true,
                total: response.data.totalElements,
              };
            }
            return {
              data: [],
              success: false,
              total: 0,
            };
          } catch (error) {
            console.error('Error fetching data:', error);
            return {
              data: [],
              success: false,
              total: 0,
            };
          }
        }}
        rowKey="id"
        search={{
          labelWidth: 'auto',
        }}
        pagination={{
          pageSize: 10,
          showQuickJumper: true,
        }}
        dateFormatter="string"
        headerTitle="图书ES管理"
        toolBarRender={() => []}
      />
    </PageContainer>
  );
} 