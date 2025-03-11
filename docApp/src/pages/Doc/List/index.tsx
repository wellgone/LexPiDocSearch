import { PlusOutlined, InfoCircleOutlined, EyeOutlined, FilePdfOutlined, EditOutlined, DeleteOutlined, ImportOutlined, CloudDownloadOutlined, DownloadOutlined, DisconnectOutlined } from '@ant-design/icons';
import type { ProColumns, ActionType } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, message, Modal, Tooltip, Upload, Progress } from 'antd';
import { useState, useRef, useEffect } from 'react';
import request from '@/utils/request';
import type { DocItem } from './data';
import DocForm from './components/DocForm';
import { getPreviewUrl, batchExtractAndImport, getBatchExtractImportStatus } from '@/services/doc';

const DocList: React.FC = () => {
  const [createModalVisible, handleModalVisible] = useState<boolean>(false);
  const [currentRow, setCurrentRow] = useState<DocItem>();
  const actionRef = useRef<ActionType>();
  const [importLoading, setImportLoading] = useState<boolean>(false);
  const [batchOpacStatus, setBatchOpacStatus] = useState<string>('');
  const [batchOpacModalVisible, setBatchOpacModalVisible] = useState<boolean>(false);
  const [importStatus, setImportStatus] = useState<string>('');
  const [importModalVisible, setImportModalVisible] = useState<boolean>(false);
  const [extractProgress, setExtractProgress] = useState<any>(null);
  const [extractModalVisible, setExtractModalVisible] = useState<boolean>(false);
  const [importProgress, setImportProgress] = useState<any>(null);
  const [opacProgress, setOpacProgress] = useState<any>(null);
  const [extractBatchModalVisible, setExtractBatchModalVisible] = useState<boolean>(false);
  const [extractBatchProgress, setExtractBatchProgress] = useState<any>(null);
  const [extractBatchStatus, setExtractBatchStatus] = useState<string>('');
  const [categories, setCategories] = useState<Record<string, { text: string }>>({});

  // 批量解析OPAC主题标签相关状态
  const [batchParseTopicModalVisible, setBatchParseTopicModalVisible] = useState(false);
  const [batchParseTopicStatus, setBatchParseTopicStatus] = useState('');
  const [parseTopicProgress, setParseTopicProgress] = useState<any>(null);

  // 获取分类数据
  const fetchCategories = async () => {
    try {
      const response = await request('/api/doc/categories', {
        method: 'GET',
      });
      if (response.code === 0 && Array.isArray(response.data)) {
        // 转换为 valueEnum 格式
        const formattedCategories = response.data.reduce((acc: Record<string, { text: string }>, category: string) => {
          acc[category] = { text: category };
          return acc;
        }, {});
        setCategories(formattedCategories);
      }
    } catch (error) {
      console.error('获取分类数据失败:', error);
      message.error('获取分类数据失败');
    }
  };

  // 在组件挂载时获取分类数据
  useEffect(() => {
    fetchCategories();
  }, []);

  const handleAdd = async (fields: DocItem) => {
    const hide = message.loading('正在添加');
    try {
      await request('/api/doc/add', {
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

  const handleUpdate = async (fields: DocItem) => {
    const hide = message.loading('正在更新');
    try {
      await request('/api/doc/update', {
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

  const handleRemove = async (id: number) => {
    const hide = message.loading('正在删除');
    try {
      await request(`/api/doc/delete/${id}`, {
        method: 'DELETE',
      });
      hide();
      message.success('删除成功');
      // 删除成功后刷新列表
      if (actionRef.current) {
        actionRef.current.reload();
      }
      return true;
    } catch (error) {
      hide();
      message.error('删除失败，请重试');
      return false;
    }
  };

  const handlePreview = async (id: number) => {
    try {
      const response = await getPreviewUrl(id);
      if (response.code === 0 && response.data) {
        window.open('/api3/'+response.data, '_blank');
      } else {
        message.error(response.message || '获取预览地址失败');
      }
    } catch (error) {
      message.error('获取预览地址失败');
    }
  };

  const handleExtract = async (id: number) => {
    const hide = message.loading('正在初始化抽取任务...');
    try {
      const response = await request(`/api/doc/extract/${id}`, {
        method: 'POST',
      });
      hide();
      
      if (response.code === 20001) {
        // 获取任务ID
        const taskId = response.message.split('任务ID:')[1];
        setExtractModalVisible(true);
        setExtractProgress({
          status: 0,
          progress: 0,
          currentStep: '初始化任务'
        });
        
        // 每1000ms查询一次抽取结果
        const interval = setInterval(async () => {
          try {
            const resultResponse = await request(`/api/doc/extract/result/${taskId}`, {
              method: 'GET',
            });
            
            if (resultResponse.code === 0) {
              const progress = resultResponse.data;
              setExtractProgress(progress);
              
              // 如果任务完成或失败
              if (progress.status !== 0) {
                clearInterval(interval);
                if (progress.status === 1) {
                  message.success('抽取成功');
                  actionRef.current?.reload();
                } else {
                  message.error('抽取失败: ' + progress.errorMessage);
                }
              }
            } else {
              clearInterval(interval);
              setExtractProgress(null);
              message.error('获取进度失败');
            }
          } catch (error) {
            clearInterval(interval);
            setExtractProgress(null);
            message.error('获取进度失败');
          }
        }, 1000);
      } else {
        message.error(response.message || '抽取失败');
      }
    } catch (error) {
      hide();
      message.error('抽取失败，请重试');
    }
  };

  // 导入章节到ES
  const handleImportToEs = async (record: DocItem) => {
    const hide = message.loading('正在导入...');
    try {
      await request(`/api/es/docs/import/doc/${record.id}`, {
        method: 'POST',
      });
      hide();
      message.success('导入成功');
      actionRef.current?.reload();
    } catch (error) {
      hide();
      message.error('导入失败，请重试');
    }
  };

  // 处理Excel文件上传
  const handleExcelUpload = async (file: File) => {
    Modal.confirm({
      title: '确认导入',
      content: '确定要导入Excel文件吗？导入过程中会自动提取PDF首页作为封面。',
      onOk: async () => {
        setImportLoading(true);
        setImportModalVisible(true);
        setImportProgress({
          status: 0,
          progress: 0,
          currentStep: '正在上传文件...'
        });
        
        const formData = new FormData();
        formData.append('file', file);

        try {
          const response = await request('/api/doc/import/excel', {
            method: 'POST',
            data: formData,
          });

          if (response.code === 20001) {
            // 获取任务ID
            const taskId = response.message.split('任务ID:')[1];
            
            // 每1000ms查询一次导入结果
            const interval = setInterval(async () => {
              try {
                const resultResponse = await request(`/api/doc/import/status/${taskId}`, {
                  method: 'GET',
                });
                
                if (resultResponse.code === 0) {
                  const progress = resultResponse.data;
                  setImportProgress(progress);
                  setImportStatus(`${progress.currentStep}`);
                  
                  // 如果任务完成或失败
                  if (progress.status !== 0) {
                    clearInterval(interval);
                    if (progress.status === 1) {
                      setImportStatus(progress.result || '导入完成');
                      message.success('导入完成');
                      actionRef.current?.reload();
                    } else {
                      setImportStatus(progress.errorMessage || '导入失败');
                      message.error('导入失败: ' + progress.errorMessage);
                    }
                    setImportLoading(false);
                  }
                } else {
                  clearInterval(interval);
                  setImportProgress(null);
                  setImportStatus('获取进度失败');
                  message.error('获取进度失败');
                  setImportLoading(false);
                }
              } catch (error) {
                clearInterval(interval);
                setImportProgress(null);
                setImportStatus('导入过程中发生错误');
                message.error('导入失败，请重试');
                setImportLoading(false);
              }
            }, 1000);
          } else {
            setImportProgress(null);
            setImportStatus(response.message || '导入失败');
            message.error(response.message || '导入失败');
            setImportLoading(false);
          }
        } catch (error) {
          setImportProgress(null);
          setImportStatus('导入失败，请重试');
          message.error('导入失败，请重试');
          setImportLoading(false);
        }
      }
    });
  };

  // 处理获取OPAC信息
  const handleGetOpac = async (id: number) => {
    const hide = message.loading('正在初始化OPAC获取任务...');
    try {
      const response = await request(`/api/doc/opac/${id}`, {
        method: 'POST',
      });
      hide();

      if (response.code === 20001) {
        // 获取任务ID
        const taskId = response.message.split('任务ID:')[1];
        setBatchOpacModalVisible(true);
        setOpacProgress({
          status: 0,
          progress: 0,
          currentStep: '初始化任务...'
        });
        
        // 每1000ms查询一次获取结果
        const interval = setInterval(async () => {
          try {
            const resultResponse = await request(`/api/doc/opac/status/${taskId}`, {
              method: 'GET',
            });
            
            if (resultResponse.code === 0) {
              const progress = resultResponse.data;
              setOpacProgress(progress);
              setBatchOpacStatus(`${progress.currentStep}`);
              
              // 如果任务完成或失败
              if (progress.status !== 0) {
                clearInterval(interval);
                if (progress.status === 1) {
                  setBatchOpacStatus(progress.result || '获取完成');
                  message.success('获取成功');
                  actionRef.current?.reload();
                } else {
                  setBatchOpacStatus(progress.errorMessage || '获取失败');
                  message.error('获取失败: ' + progress.errorMessage);
                }
              }
            } else {
              clearInterval(interval);
              setOpacProgress(null);
              setBatchOpacStatus('获取进度失败');
              message.error('获取进度失败');
            }
          } catch (error) {
            clearInterval(interval);
            setOpacProgress(null);
            setBatchOpacStatus('获取进度失败');
            message.error('获取进度失败');
          }
        }, 1000);
      } else {
        message.error(response.message || '获取失败');
      }
    } catch (error) {
      hide();
      message.error('获取失败，请重试');
    }
  };

  const handleBatchGetOpacInfo = async () => {
    Modal.confirm({
      title: '确认批量获取',
      content: '确定要批量获取所有未获取OPAC信息的图书信息吗？',
      onOk: async () => {
        setBatchOpacStatus('正在初始化任务...');
        setBatchOpacModalVisible(true);
        
        try {
          const response = await request('/api/doc/opac/batch', {
            method: 'POST',
          });

          if (response.code === 20001) {
            // 获取任务ID
            const taskId = response.message.split('任务ID:')[1];
            
            // 开始轮询任务状态
            const interval = setInterval(async () => {
              try {
                const resultResponse = await request(`/api/doc/opac/batch/status/${taskId}`, {
                  method: 'GET',
                });
                
                if (resultResponse.code === 0) {
                  const progress = resultResponse.data;
                  setOpacProgress(progress);
                  // setBatchOpacStatus(`${progress.currentStep} (${progress.progress}%)`);
                  setBatchOpacStatus(`${progress.currentStep}`);
                  
                  // 如果任务完成或失败
                  if (progress.status !== 0) {
                    clearInterval(interval);
                    if (progress.status === 1) {
                      setBatchOpacStatus(progress.result || '批量获取完成');
                      message.success('批量获取完成');
                      actionRef.current?.reload();
                    } else {
                      setBatchOpacStatus(progress.errorMessage || '批量获取失败');
                      message.error('批量获取失败: ' + progress.errorMessage);
                    }
                  }
                } else {
                  clearInterval(interval);
                  setBatchOpacStatus('获取进度失败');
                  message.error('获取进度失败');
                }
              } catch (error) {
                clearInterval(interval);
                setBatchOpacStatus('获取进度失败');
                message.error('获取进度失败');
              }
            }, 1000);
          } else {
            setBatchOpacStatus('初始化任务失败');
            message.error(response.message || '批量获取失败');
          }
        } catch (error) {
          setBatchOpacStatus('初始化任务失败');
          message.error('批量获取失败');
        }
      },
    });
  };

  // 批量解析OPAC主题标签
  const handleBatchParseOPACTopics = async () => {
    Modal.confirm({
      title: '确认批量解析',
      content: '确定要批量解析所有未处理的OPAC主题标签吗？',
      onOk: async () => {
        setBatchParseTopicStatus('正在初始化任务...');
        setBatchParseTopicModalVisible(true);
        
        try {
          const response = await request('/api/doc/opac/topics/batch', {
            method: 'POST',
          });

          if (response.code === 20001) {
            // 获取任务ID
            const taskId = response.message.split('任务ID:')[1];
            
            // 开始轮询任务状态
            const interval = setInterval(async () => {
              try {
                const resultResponse = await request(`/api/doc/opac/topics/batch/status/${taskId}`, {
                  method: 'GET',
                });
                
                if (resultResponse.code === 0) {
                  const progress = resultResponse.data;
                  setParseTopicProgress(progress);
                  setBatchParseTopicStatus(`${progress.currentStep}`);
                  
                  // 如果任务完成或失败
                  if (progress.status !== 0) {
                    clearInterval(interval);
                    if (progress.status === 1) {
                      setBatchParseTopicStatus(progress.result || '批量解析完成');
                      message.success('批量解析完成');
                      actionRef.current?.reload();
                    } else {
                      setBatchParseTopicStatus(progress.errorMessage || '批量解析失败');
                      message.error('批量解析失败: ' + progress.errorMessage);
                    }
                  }
                }
              } catch (error) {
                clearInterval(interval);
                setBatchParseTopicStatus('查询任务状态失败');
                message.error('查询任务状态失败');
                console.error(error);
              }
            }, 2000); // 每2秒查询一次
          } else {
            setBatchParseTopicStatus('初始化任务失败');
            message.error(response.message || '初始化任务失败');
          }
        } catch (error) {
          setBatchParseTopicStatus('初始化任务失败');
          message.error('初始化任务失败');
          console.error(error);
        }
      },
    });
  };

  const handleBatchExtractAndImport = async () => {
    try {
      setExtractBatchModalVisible(true);
      setExtractBatchProgress({
        status: 0,
        progress: 0,
        currentStep: '初始化任务...'
      });
      
      const result = await batchExtractAndImport();
      if (result.code === 20001) {
        const taskId = result.message.split(':')[1].trim();
        message.success('批量提取任务已开始');
        
        // 开始轮询任务状态
        const interval = setInterval(async () => {
          const statusResult = await getBatchExtractImportStatus(taskId);
          if (statusResult.code === 0) {
            const progress = statusResult.data;
            setExtractBatchProgress(progress);
            setExtractBatchStatus(progress.currentStep);
            
            if (progress.status === 1) {
              // 任务完成
              clearInterval(interval);
              setExtractBatchStatus(progress.result || '批量提取完成');
              message.success('批量提取完成');
              actionRef.current?.reload();
            } else if (progress.status === 2) {
              // 任务失败
              clearInterval(interval);
              setExtractBatchStatus(progress.errorMessage || '批量提取失败');
              message.error(`批量提取失败: ${progress.errorMessage}`);
            }
          }
        }, 1000);
      } else {
        setExtractBatchProgress(null);
        setExtractBatchStatus(result.message || '批量提取失败');
        message.error(result.message || '批量提取失败');
      }
    } catch (error) {
      setExtractBatchProgress(null);
      setExtractBatchStatus('批量提取失败');
      message.error('批量提取失败');
    }
  };

  const handleDeleteIndex = async (record: DocItem) => {
    try {
      const response = await request(`/api/es/docs/doc/${record.id}`, {
        method: 'DELETE',
      });
      if (response.code === 0) {
        message.success('索引删除成功');
        // 刷新表格数据
        if (actionRef.current) {
          actionRef.current.reload();
        }
      } else {
        message.error(response.message || '删除索引失败');
      }
    } catch (error) {
      console.error('删除索引失败:', error);
      message.error('删除索引失败');
    }
  };

  const columns: ProColumns<DocItem>[] = [
    {
      title: '文档名称',
      dataIndex: 'title',
      width: 200,
      ellipsis: true,
      fixed: 'left',
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 80,
      valueEnum: {
        1: { text: '图书' },
        2: { text: '自定义PDF' },
      },
    },
    {
      title: '副标题',
      dataIndex: 'subTitle',
      width: 200,
      ellipsis: true,
      // 不显示
      hidden: true,
      search: false,
    },
    {
      title: '作者',
      dataIndex: 'author',
      width: 120,
    },
    {
      title: '出版社',
      dataIndex: 'publisher',
      width: 150,
    },
    {
      title: '出版年份',
      dataIndex: 'publicationYear',
      width: 100,
      search: false,
    },
    {
      title: '出版日期',
      dataIndex: 'publicationDate',
      width: 100,
      search: false,
    },
    {
      title: 'OCR',
      dataIndex: 'isOcr',
      width: 50,
      search: false,
      valueEnum: {
        0: { text: '', status: 'Error' },
        1: { text: '', status: 'Success' },
      },
    },
    {
      title: '索引',
      dataIndex: 'isIndexed',
      width: 50,
      search: false,
      valueEnum: {
        0: { text: '', status: 'Error' },
        1: { text: '', status: 'Success' },
      },
    },
    {
      title: 'OPAC',
      dataIndex: 'isOpaced',
      search: false,
      width: 50,
      valueEnum: {
        0: { text: '', status: 'Error' },
        1: { text: '', status: 'Success' },
      },
    },
    {
      title: 'ISBN',
      dataIndex: 'isbn',
      width: 130,
    },
    {
      title: '分类',
      dataIndex: 'category',
      width: 100,
      valueEnum: categories,
    },
    {
      title: '关键词',
      dataIndex: 'keyWord',
      width: 150,
      ellipsis: true,
      search: false,
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      width: 200,
      ellipsis: true,
      hidden: true,
      search: false,
    },
    {
      title: '备注',
      dataIndex: 'note',
      width: 150,
      ellipsis: true,
      search: false,
    },
    {
      title: '来源',
      dataIndex: 'source',
      width: 120,
      search: false
    },
    {
      title: '丛编',
      dataIndex: 'series',
      width: 120,
      search: true,
    },
    {
      title: '页数',
      dataIndex: 'pageSize',
      width: 80,
      search: false,
    },
    {
      title: '中图号',
      dataIndex: 'cn',
      width: 120,
      search: false,
    },
    {
      title: '主题',
      dataIndex: 'topic',
      width: 150,
      ellipsis: true,
      search: false,
    },
    {
      title: 'OPAC丛编',
      dataIndex: 'opacSeries',
      width: 150,
      search: false,
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
      valueType: 'option',
      width: 180,
      fixed: 'right',
      render: (_, record) => [
        <Tooltip title="编辑" key="edit-tooltip">
          <Button
            type="link"
            key="edit"
            icon={<EditOutlined />}
            onClick={() => {
              setCurrentRow(record);
              handleModalVisible(true);
            }}
          />
        </Tooltip>,
        <Tooltip title="抽取" key="extract-tooltip">
          <Button
            key="extract"
            type="link"
            icon={<FilePdfOutlined />}
            disabled={record.isOcr === 1}
            onClick={() => {
              Modal.confirm({
                title: '确认抽取',
                content: '确定要对该书进行内容抽取吗？',
                onOk: () => handleExtract(record.id!),
              });
            }}
          />
        </Tooltip>,
        // 根据是否已索引显示不同的按钮
        record.isIndexed === 1 ? (
          <Tooltip title="删除索引" key="delete-index-tooltip">
            <Button
              key="delete-index"
              type="link"
              icon={<DisconnectOutlined />}
              onClick={() => {
                Modal.confirm({
                  title: '确认删除索引',
                  content: '确定要删除该书的所有索引吗？',
                  onOk: () => handleDeleteIndex(record),
                });
              }}
            />
          </Tooltip>
        ) : (
          <Tooltip title="导入" key="import-tooltip">
            <Button
              key="import"
              type="link"
              icon={<ImportOutlined />}
              onClick={() => {
                Modal.confirm({
                  title: '确认导入',
                  content: '确定要导入该书的所有章节到ES吗？',
                  onOk: () => handleImportToEs(record),
                });
              }}
            />
          </Tooltip>
        ),
        <Tooltip title="获取OPAC" key="opac-tooltip">
          <Button
            key="opac"
            type="link"
            icon={<CloudDownloadOutlined />}
            disabled={record.isOpaced === 1}
            onClick={() => {
              Modal.confirm({
                title: '确认获取',
                content: '确定要获取该书的OPAC信息吗？',
                onOk: () => handleGetOpac(record.id!),
              });
            }}
          />
        </Tooltip>,
        <Tooltip title="预览" key="preview-tooltip">
          <Button
            type="link"
            key="preview"
            icon={<EyeOutlined />}
            disabled={!record.fileName}
            onClick={() => handlePreview(record.id!)}
          />
        </Tooltip>,
        <Tooltip title="删除" key="delete-tooltip">
          <Button
            type="link"
            key="delete"
            icon={<DeleteOutlined />}
            onClick={() => {
              Modal.confirm({
                title: '确认删除',
                content: '确定要删除该书吗？',
                onOk: () => handleRemove(record.id!),
              });
            }}
          />
        </Tooltip>,
      ],
    },
  ];

  const toolBarRender = () => [
    <Upload
      key="upload"
      accept=".xlsx,.xls"
      showUploadList={false}
      beforeUpload={(file) => {
        handleExcelUpload(file);
        return false;
      }}
    >
      <Button loading={importLoading}> 
      <DownloadOutlined /> 导入
      <Tooltip title="请上传 Excel 文件（.xlsx 或 .xls 格式）进行批量导入；注意书名不得包含冒号，如包含则默认用空格替代。" placement="top">
          <InfoCircleOutlined style={{ color: 'red' }} />
        </Tooltip>
      </Button>
    </Upload>,
    <Button
      type="default"
      key="default"
      onClick={handleBatchGetOpacInfo}
    >
      <CloudDownloadOutlined /> 批量获取OPAC
    </Button>,
    <Button 
      type="default"
      key="batchParseTopics" 
      onClick={handleBatchParseOPACTopics}
      icon={<ImportOutlined />}
    >
      批量解析主题标签
    </Button>,
    <Button
      key="batchExtract"
      onClick={() => {
        Modal.confirm({
          title: '批量提取',
          content: '确定要批量提取所有未处理的图书吗？注意：提取过程可能需要较长时间，请耐心等待，切勿重复提交任务。',
          onOk: () => handleBatchExtractAndImport(),
        });
      }}
    >
      <FilePdfOutlined /> 提取&索引
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
  ];

  return (
    <PageContainer>
      <ProTable<DocItem>
        headerTitle="图书列表"
        rowKey="id"
        scroll={{ x: 3000 }}
        search={{
          labelWidth: 120,
          defaultCollapsed: false,
          span: 6,
        }}
        toolBarRender={toolBarRender}
        request={async (params) => {
          const { current, pageSize, ...restParams } = params;
          const response = await request('/api/doc/list', {
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
      <DocForm
        visible={createModalVisible}
        onVisibleChange={(visible: boolean) => {
          handleModalVisible(visible);
          if (!visible) {
            setCurrentRow(undefined);
          }
        }}
        onFinish={async (values: DocItem) => {
          let success;
          if (currentRow?.id) {
            success = await handleUpdate({ ...currentRow, ...values });
          } else {
            success = await handleAdd(values);
          }
          if (success) {
            handleModalVisible(false);
            setCurrentRow(undefined);
            // 刷新表格数据
            if (actionRef.current) {
              actionRef.current.reload();
            }
          }
          return success;
        }}
        values={currentRow}
      />
      <Modal
        title="文本抽取进度"
        open={extractModalVisible}
        footer={null}
        onCancel={() => {
          setExtractModalVisible(false);
          setExtractProgress(null);
        }}
        width={600}
        bodyStyle={{ 
          maxHeight: 'calc(100vh - 200px)',
          overflowY: 'auto',
          padding: '24px'
        }}
      >
        <div style={{ marginBottom: '24px' }}>
          <div style={{ marginBottom: '16px' }}>
            <Progress 
              percent={extractProgress?.progress || 0} 
              status={
                !extractProgress ? 'normal' :
                extractProgress.status === 0 ? 'active' :
                extractProgress.status === 1 ? 'success' : 'exception'
              }
              strokeWidth={10}
            />
          </div>
          <div style={{ marginBottom: '8px', fontSize: '14px', color: 'rgba(0, 0, 0, 0.85)' }}>
            {extractProgress?.currentStep}
          </div>
          <div style={{ fontSize: '12px', color: 'rgba(0, 0, 0, 0.45)', marginBottom: '16px' }}>
            关闭进度窗口不影响后台任务的执行，但切勿重复提交任务。
          </div>
          {extractProgress?.status === 1 && extractProgress.result && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#f6ffed',
              border: '1px solid #b7eb8f',
              borderRadius: '4px'
            }}>
              <p style={{ whiteSpace: 'pre-wrap' }}>{extractProgress.result}</p>
            </div>
          )}
          {extractProgress?.status === 2 && extractProgress.errorMessage && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#fff2f0',
              border: '1px solid #ffccc7',
              borderRadius: '4px'
            }}>
              <p style={{ color: '#ff4d4f', whiteSpace: 'pre-wrap' }}>{extractProgress.errorMessage}</p>
            </div>
          )}
        </div>
      </Modal>
      <Modal
        title="批量获取OPAC"
        open={batchOpacModalVisible}
        footer={null}
        onCancel={() => setBatchOpacModalVisible(false)}
        width={600}
        bodyStyle={{ 
          maxHeight: 'calc(100vh - 200px)',
          overflowY: 'auto',
          padding: '24px'
        }}
      >
        <div style={{ marginBottom: '24px' }}>
          <div style={{ marginBottom: '16px' }}>
            <Progress 
              percent={opacProgress?.progress || 0} 
              status={
                !opacProgress ? 'normal' :
                opacProgress.status === 0 ? 'active' :
                opacProgress.status === 1 ? 'success' : 'exception'
              }
              strokeWidth={10}
            />
          </div>
          <div style={{ marginBottom: '8px', fontSize: '14px', color: 'rgba(0, 0, 0, 0.85)' }}>
            {batchOpacStatus && opacProgress && batchOpacStatus !== opacProgress.result ? batchOpacStatus : null}
          </div>
          <div style={{ fontSize: '12px', color: 'rgba(0, 0, 0, 0.45)', marginBottom: '16px' }}>
            关闭进度窗口不影响后台任务的执行，但切勿重复提交任务。
          </div>
          {opacProgress?.status === 1 && opacProgress.result && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#f6ffed',
              border: '1px solid #b7eb8f',
              borderRadius: '4px'
            }}>
              <div 
                style={{ whiteSpace: 'pre-wrap' }}
                dangerouslySetInnerHTML={{ __html: opacProgress.result.replace(/\n/g, '<br>') }} 
              />
            </div>
          )}
          {opacProgress?.status === 2 && opacProgress.errorMessage && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#fff2f0',
              border: '1px solid #ffccc7',
              borderRadius: '4px'
            }}>
              <p style={{ color: '#ff4d4f', whiteSpace: 'pre-wrap' }}>{opacProgress.errorMessage}</p>
            </div>
          )}
        </div>
      </Modal>

      <Modal
        title="批量解析OPAC主题标签进度"
        open={batchParseTopicModalVisible}
        footer={null}
        onCancel={() => setBatchParseTopicModalVisible(false)}
        width={600}
        bodyStyle={{ 
          maxHeight: 'calc(100vh - 200px)',
          overflowY: 'auto',
          padding: '24px'
        }}
      >
        <div style={{ marginBottom: '24px' }}>
          <div style={{ marginBottom: '16px' }}>
            <Progress 
              percent={parseTopicProgress?.progress || 0} 
              status={
                !parseTopicProgress ? 'normal' :
                parseTopicProgress.status === 0 ? 'active' :
                parseTopicProgress.status === 1 ? 'success' : 'exception'
              }
              strokeWidth={10}
            />
          </div>
          <div style={{ marginBottom: '8px', fontSize: '14px', color: 'rgba(0, 0, 0, 0.85)' }}>
            {batchParseTopicStatus && parseTopicProgress && batchParseTopicStatus !== parseTopicProgress.result ? batchParseTopicStatus : null}
          </div>
          <div style={{ fontSize: '12px', color: 'rgba(0, 0, 0, 0.45)', marginBottom: '16px' }}>
            关闭进度窗口不影响后台任务的执行，但切勿重复提交任务。
          </div>
          {parseTopicProgress?.status === 1 && parseTopicProgress.result && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#f6ffed',
              border: '1px solid #b7eb8f',
              borderRadius: '4px'
            }}>
              <div 
                style={{ whiteSpace: 'pre-wrap' }}
                dangerouslySetInnerHTML={{ __html: parseTopicProgress.result.replace(/\n/g, '<br>') }} 
              />
            </div>
          )}
          {parseTopicProgress?.status === 2 && parseTopicProgress.errorMessage && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#fff2f0',
              border: '1px solid #ffccc7',
              borderRadius: '4px'
            }}>
              <p style={{ color: '#ff4d4f', whiteSpace: 'pre-wrap' }}>{parseTopicProgress.errorMessage}</p>
            </div>
          )}
        </div>
      </Modal>
      <Modal
        title="批量导入进度"
        open={importModalVisible}
        footer={null}
        onCancel={() => setImportModalVisible(false)}
        width={600}
        bodyStyle={{ 
          maxHeight: 'calc(100vh - 200px)',
          overflowY: 'auto',
          padding: '24px'
        }}
      >
        <div style={{ marginBottom: '24px' }}>
          <div style={{ marginBottom: '16px' }}>
            <Progress 
              percent={importProgress?.progress || 0} 
              status={
                !importProgress ? 'normal' :
                importProgress.status === 0 ? 'active' :
                importProgress.status === 1 ? 'success' : 'exception'
              }
              strokeWidth={10}
            />
          </div>
          <div style={{ marginBottom: '8px', fontSize: '14px', color: 'rgba(0, 0, 0, 0.85)' }}>
            {importStatus && importStatus !== importProgress.result ? importStatus : null}
          </div>
          <div style={{ fontSize: '12px', color: 'rgba(0, 0, 0, 0.45)', marginBottom: '16px' }}>
            关闭进度窗口不影响后台任务的执行，但切勿重复提交任务。
          </div>
          {importProgress?.status === 1 && importProgress.result && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#f6ffed',
              border: '1px solid #b7eb8f',
              borderRadius: '4px'
            }}>
              <div 
                style={{ whiteSpace: 'pre-wrap' }}
                dangerouslySetInnerHTML={{ __html: importProgress.result.replace(/\n/g, '<br>') }} 
              />
            </div>
          )}
          {importProgress?.status === 2 && importProgress.errorMessage && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#fff2f0',
              border: '1px solid #ffccc7',
              borderRadius: '4px'
            }}>
              <p style={{ color: '#ff4d4f', whiteSpace: 'pre-wrap' }}>{importProgress.errorMessage}</p>
            </div>
          )}
        </div>
      </Modal>
      <Modal
        title="批量提取进度"
        open={extractBatchModalVisible}
        footer={null}
        onCancel={() => setExtractBatchModalVisible(false)}
        width={600}
        bodyStyle={{ 
          maxHeight: 'calc(100vh - 200px)',
          overflowY: 'auto',
          padding: '24px'
        }}
      >
        <div style={{ marginBottom: '24px' }}>
          <div style={{ marginBottom: '16px' }}>
            <Progress 
              percent={extractBatchProgress?.progress || 0} 
              status={
                !extractBatchProgress ? 'normal' :
                extractBatchProgress.status === 0 ? 'active' :
                extractBatchProgress.status === 1 ? 'success' : 'exception'
              }
              strokeWidth={10}
            />
          </div>
          <div style={{ marginBottom: '8px', fontSize: '14px', color: 'rgba(0, 0, 0, 0.85)' }}>
            {extractBatchStatus && extractBatchProgress && extractBatchStatus !== extractBatchProgress.result ? extractBatchStatus : null}
          </div>
          {extractBatchProgress?.status === 1 && extractBatchProgress.result && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#f6ffed',
              border: '1px solid #b7eb8f',
              borderRadius: '4px'
            }}>
              <div 
                style={{ whiteSpace: 'pre-wrap' }}
                dangerouslySetInnerHTML={{ __html: extractBatchProgress.result.replace(/\n/g, '<br>') }} 
              />
            </div>
          )}
          {extractBatchProgress?.status === 2 && extractBatchProgress.errorMessage && (
            <div style={{ 
              marginTop: '16px',
              padding: '16px',
              background: '#fff2f0',
              border: '1px solid #ffccc7',
              borderRadius: '4px'
            }}>
              <p style={{ color: '#ff4d4f', whiteSpace: 'pre-wrap' }}>{extractBatchProgress.errorMessage}</p>
            </div>
          )}
        </div>
      </Modal>
    </PageContainer>
  );
};

export default DocList; 
