import { ModalForm, ProFormText, ProFormTextArea, ProFormSelect, ProFormDigit } from '@ant-design/pro-components';
import type { DocFormProps, DocItem } from '../data';
import { message, Row, Col, Upload, TreeSelect, Form, Select } from 'antd';
import type { UploadProps } from 'antd/es/upload/interface';
import { InboxOutlined } from '@ant-design/icons';
import { useEffect, useRef, useState } from 'react';
import type { ProFormInstance } from '@ant-design/pro-components';
import { getPreviewUrl } from '@/services/doc';
import ImageUpload from '@/components/ImageUpload';
import request from '@/utils/request';

// 定义主题标签节点类型
interface TopicNode {
  id: string;
  parentId?: string;
  title: string;
  value: string;
  key: string;
  level: number;
  children: TopicNode[];
}

const DocForm: React.FC<DocFormProps> = (props) => {
  const { visible, onVisibleChange, onFinish, values } = props;
  const formRef = useRef<ProFormInstance>();
  const [coverUrl, setCoverUrl] = useState<string>();
  const [topicTreeData, setTopicTreeData] = useState<TopicNode[]>([]);
  const [selectedTopics, setSelectedTopics] = useState<string[]>([]);
  const [loadingTopics, setLoadingTopics] = useState<boolean>(false);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [tagOptions, setTagOptions] = useState<{ label: string; value: string }[]>([]);
  const [categories, setCategories] = useState<{ label: string; value: string }[]>([]);

  function processTopics(data: { topicId: number; levelSize: number; path: string }[]) {
    // 按 levelSize 从大到小排序
    let sortedData = data.sort((a, b) => b.levelSize - a.levelSize);
    
    // 用于存储已删除的路径
    const deletedPaths = new Set<string>();

    // 遍历 levelSize 从 4 到 1
    for (let level = 4; level >= 1; level--) {
        console.log("===level",level);
        // 找出当前 level 的所有对象
        const currentLevelItems = sortedData.filter(item => item.levelSize === level);
        
        // 找出当前 level 的父路径并去重
        const parentPaths = new Set<string>();
        currentLevelItems.forEach(item => {
            const pathParts = item.path.split(' > ');
            if (pathParts.length > 1) {
                pathParts.pop(); // 去掉最后一个部分，得到父路径
                parentPaths.add(pathParts.join(' > '));
            }
        });
        console.log("===parentPaths",parentPaths);
        // 从下一个 level 中删除与父路径匹配的对象
        const nextLevel = level - 1;
        if (nextLevel >= 0) {
            // 使用 filter 方法创建一个新的数组，避免直接修改 sortedData
            sortedData = sortedData.filter(item => {
                if (item.levelSize === nextLevel && parentPaths.has(item.path)) {
                    deletedPaths.add(item.path); // 记录已删除的路径
                    return false; // 删除该项
                }
                return true; // 保留该项
            });
        }
        parentPaths.forEach(path => {
            console.log("===path",path);
            const pathParts = path.split(' > ');
            if (pathParts.length > 1) {
                pathParts.pop(); // 去掉最后一个部分，得到父路径
                parentPaths.add(pathParts.join(' > '));
            }
        });

        // 从下下一个 level 中删除与父路径匹配的对象
        const nextLevel2 = level - 2;
        if (nextLevel2 >= 0) {
            // 使用 filter 方法创建一个新的数组，避免直接修改 sortedData
            sortedData = sortedData.filter(item => {
                if (item.levelSize === nextLevel2 && parentPaths.has(item.path)) {
                    deletedPaths.add(item.path); // 记录已删除的路径
                    return false; // 删除该项
                }
                return true; // 保留该项
            });
        }
    }
    //删除sortedData中level=0对象
    sortedData = sortedData.filter(item => item.levelSize !== 0);
    console.log("===sortedData",sortedData);

    return sortedData;
}

  // 将扁平数据转换为树形结构
  const convertToTreeData = (data: any[]): TopicNode[] => {
    try {
      // 创建一个新的数组来存储处理后的数据
      const processedData = data.map(item => ({
        id: String(item.id),         // 将ID转换为字符串
        parentId: item.parentId ? String(item.parentId) : undefined,  // 父ID也转换为字符串
        title: item.name,
        value: String(item.id),      // value也使用字符串ID
        key: String(item.id),        // key使用字符串ID
        level: item.level || 0,
        children: [],
      })) as TopicNode[];

      // 创建一个Map来存储id到节点的映射
      const idToNodeMap = new Map(
        processedData.map(item => [item.id, item])
      );

      // 用于存储根节点的数组
      const roots: TopicNode[] = [];

      // 遍历处理后的数据，构建树形结构
      processedData.forEach(node => {
        if (node.parentId && idToNodeMap.has(node.parentId)) {
          // 如果有父节点，将当前节点添加到父节点的children中
          const parent = idToNodeMap.get(node.parentId);
          if (parent && !parent.children.some(child => child.id === node.id)) {
            parent.children.push(node);
          }
        } else {
          // 如果没有父节点或父节点不存在，作为根节点
          if (!roots.some(root => root.id === node.id)) {
            roots.push(node);
          }
        }
      });

      // 按层级和标题排序
      const sortNodes = (nodes: TopicNode[]): TopicNode[] => {
        return nodes.sort((a, b) => {
          if (a.level !== b.level) {
            return a.level - b.level;
          }
          return (a.title || '').localeCompare(b.title || '', 'zh-CN');
        });
      };

      // 递归对每一层进行排序
      const sortTreeRecursive = (nodes: TopicNode[]): TopicNode[] => {
        sortNodes(nodes);
        nodes.forEach(node => {
          if (node.children && node.children.length > 0) {
            sortTreeRecursive(node.children);
          }
        });
        return nodes;
      };

      // 返回排序后的树形结构
      return sortTreeRecursive(roots);
    } catch (error) {
      console.error('转换主题标签数据失败:', error);
      return [];
    }
  };

  // 加载封面图片
  const loadCoverImage = async (imgId: string) => {
    try {
      const response = await request(`/api/doc/cover/img/${imgId}`);
      if (response.code === 0 && response.data) {
        setCoverUrl(`data:image/jpeg;base64,${response.data}`);
      }
    } catch (error) {
      console.error('加载封面失败:', error);
    }
  };

  // 加载图书的主题标签
  const loadDocTopics = async (docId: number) => {
    try {
      const response = await request(`/api/topic/doc/${docId}/paths`, {
        method: 'GET',
      });
      if (response.code === 0 && Array.isArray(response.data)) {
        const processedTopics = processTopics( response.data);
        console.log("processedTopics",processedTopics);
        const topicIds = processedTopics.map((item: { topicId: number }) => String(item.topicId));

        setSelectedTopics(topicIds);
      }
    } catch (error) {
      console.error('加载图书主题标签失败:', error);
    }
  };

  // 加载自定义标签
  const loadCustomTags = async () => {
    try {
      const response = await request('/api/topic/tags', {
        method: 'GET',
      });
      if (response.code === 0 && response.data?.records) {
        const options = response.data.records.map((tag: any) => ({
          label: tag.name,
          value: String(tag.id),
        }));
        setTagOptions(options);
      }
    } catch (error) {
      console.error('加载自定义标签失败:', error);
      message.error('加载自定义标签失败');
    }
  };

  // 加载图书的自定义标签
  const loadDocTags = async (docId: number) => {
    try {
      const response = await request(`/api/topic/doc/${docId}/tags`, {
        method: 'GET',
      });
      if (response.code === 0 && Array.isArray(response.data)) {
        const tagIds = response.data.map((tag: any) => String(tag.id));
        setSelectedTags(tagIds);
      }
    } catch (error) {
      console.error('加载图书自定义标签失败:', error);
      message.error('加载图书自定义标签失败');
    }
  };

  // 加载主题标签树数据
  const loadTopicTree = async () => {
    setLoadingTopics(true);
    try {
      const response = await request('/api/topic/list', {
        method: 'GET',
      });
      if (response.code === 0 && Array.isArray(response.data)) {
        // 将扁平数据转换为树形结构
        console.log(response.data);
        const treeData = convertToTreeData(response.data);
        setTopicTreeData(treeData);
        
        // 如果有图书ID，加载图书的主题标签
        if (values?.id) {
          await loadDocTopics(values.id);
        }
      } else {
        message.error('加载主题标签失败：数据格式错误');
      }
    } catch (error) {
      console.error('加载主题标签失败:', error);
      message.error('加载主题标签失败');
    } finally {
      setLoadingTopics(false);
    }
  };

  // 获取分类数据
  const fetchCategories = async () => {
    try {
      const response = await request('/api/doc/categories', {
        method: 'GET',
      });
      if (response.code === 0 && Array.isArray(response.data)) {
        const formattedCategories = response.data.map((category: string) => ({
          label: category,
          value: category,
        }));
        setCategories(formattedCategories);
      }
    } catch (error) {
      console.error('获取分类数据失败:', error);
      message.error('获取分类数据失败');
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  // 加载图书文件
  const loadDocFile = async (docId: number) => {
    try {
      const response = await request(`/api/doc-file/doc/${docId}`);
      if (response.code === 0 && response.data) {
        formRef.current?.setFieldsValue({ fileId: response.data[0].fileId });
      }
    } catch (error) {
      console.error('加载图书文件失败:', error);
      // message.error('加载图书文件失败');
    }
  };
  //组件加载时，加载图书文件
  useEffect(() => {
    if (values?.id) {
      loadDocFile(values.id);
    }
  }, [values?.id]);

  // 当visible或values变化时重置表单和封面
  useEffect(() => {
    if (!visible) {
      formRef.current?.resetFields();
      setCoverUrl(undefined);
      setSelectedTopics([]);
      setSelectedTags([]);
      setTopicTreeData([]);
      setTagOptions([]);
    } else {
      // 加载主题标签树和自定义标签
      loadTopicTree();
      loadCustomTags();
      
      if (values) {
        formRef.current?.setFieldsValue(values);
        // 加载封面
        if (values.picUrl) {
          if (values.picUrl.startsWith('data:')) {
            setCoverUrl(values.picUrl);
          } else if (values.picUrl.startsWith('img/')) {
            if (values.id) {
              loadCoverImage(values.picUrl.replace('img/', ''));
            }
          } else {
            setCoverUrl('/api2/'+values.picUrl);
          }
        }
        // 如果有图书ID，加载图书的标签
        if (values.id) {
          loadDocTags(values.id);
        }
      }
    }
  }, [visible, values]);

  // 获取节点及其所有子节点的ID
  const getAllChildrenIds = (node: TopicNode): string[] => {
    const ids: string[] = [node.id];
    if (node.children && node.children.length > 0) {
      node.children.forEach(child => {
        ids.push(...getAllChildrenIds(child));
      });
    }
    return ids;
  };

  // 根据选中的ID获取所有关联的ID（包括子节点）
  const getRelatedTopicIds = (selectedIds: string[]): string[] => {
    const allIds = new Set<string>();
    
    // 遍历树形数据，找到选中的节点及其子节点
    const traverseTree = (nodes: TopicNode[]) => {
      nodes.forEach(node => {
        if (selectedIds.includes(node.id)) {
          // 将当前节点及其所有子节点的ID添加到结果集
          getAllChildrenIds(node).forEach(id => allIds.add(id));
        }
        if (node.children && node.children.length > 0) {
          traverseTree(node.children);
        }
      });
    };

    traverseTree(topicTreeData);
    return Array.from(allIds);
  };

  // 获取节点对应的全部父节点
  const getParentIds = (nodeIds: string[]): string[] => {
    const parentIdsSet = new Set<string>(); // 使用 Set 来存储唯一的父节点 ID
    // 创建节点ID到节点的映射
    const idToNodeMap = new Map(
      topicTreeData.flatMap(node => {
        const result: [string, TopicNode][] = [[node.id, node]];
        const mapChildren = (n: TopicNode) => {
          n.children?.forEach(child => {
            result.push([child.id, child]);
            mapChildren(child);
          });
        };
        mapChildren(node);
        return result;
      })
    );

    // 遍历传入的节点ID数组
    nodeIds.forEach(nodeId => {
      let currentNode = idToNodeMap.get(nodeId);
      while (currentNode?.parentId) {
        parentIdsSet.add(currentNode.parentId); // 添加到 Set 中以确保唯一性
        currentNode = idToNodeMap.get(currentNode.parentId);
      }
    });

    return Array.from(parentIdsSet); // 将 Set 转换为数组返回
  };

  // 处理主题标签变化
  const handleTopicChange = async (newTopicIds: string[]) => {
    if (!values?.id) {
      message.error('请先保存图书基本信息');
      return;
    }

    try {
      setLoadingTopics(true);
      // 获取所有关联的主题标签ID（包括子节点）
      const allRelatedIds = getRelatedTopicIds(newTopicIds);
      
      // 找出需要删除的标签
      const toDelete = selectedTopics.filter(id => !allRelatedIds.includes(id));
      // 找出需要新增的标签
      const toAdd = allRelatedIds.filter(id => !selectedTopics.includes(id));
      // 找出需要新增的父标签
      const toAddParent = getParentIds(newTopicIds);
      //将需要新增的父标签到toAdd
      toAdd.push(...toAddParent);

      // 删除取消选中的标签
      for (const topicId of toDelete) {
        await request(`/api/topic/doc/${values.id}/topic/${topicId}`, {
          method: 'DELETE',
        });
      }

      // 添加新选中的标签
      for (const topicId of toAdd) {
        await request(`/api/topic/doc/${values.id}/topic/${topicId}`, {
          method: 'POST',
        });
      }

      setSelectedTopics(allRelatedIds);
      message.success('主题标签更新成功');
    } catch (error) {
      console.error('更新主题标签失败:', error);
      message.error('更新主题标签失败');
      // 更新失败时，重新加载当前标签
      if (values.id) {
        await loadDocTopics(values.id);
      }
    } finally {
      setLoadingTopics(false);
    }
  };

  // 处理封面上传
  const handleCoverChange = async (base64Data: string) => {
    // if (!values?.id) {
    //   message.error('请先保存图书基本信息');
    //   return;
    // }

    try {
      const response = await request(`/api/doc/upload/cover`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        data: { img:base64Data }
      });

      if (response.code === 0) {
        message.success('封面上传成功');
        setCoverUrl(base64Data);
        formRef.current?.setFieldValue('picUrl', response.data);
      } else {
        message.error(response.message || '封面上传失败');
      }
    } catch (error) {
      console.error('上传封面失败:', error);
      message.error('上传封面失败');
    }
  };

  // 处理自定义标签变化
  const handleTagChange = async (newTags: string[]) => {
    if (!values?.id) {
      message.error('请先保存图书基本信息');
      return;
    }

    try {
      setLoadingTopics(true);
      
      // 找出需要删除的标签
      const toDelete = selectedTags.filter(id => !newTags.includes(id));
      // 找出需要新增的标签
      const toAdd = newTags.filter(id => !selectedTags.includes(id));

      // 删除取消选中的标签
      for (const tagId of toDelete) {
        await request(`/api/topic/doc/${values.id}/topic/${tagId}`, {
          method: 'DELETE',
        });
      }

      // 添加新选中的标签
      for (const tagId of toAdd) {
        await request(`/api/topic/doc/${values.id}/topic/${tagId}`, {
          method: 'POST',
        });
      }

      setSelectedTags(newTags);
      message.success('自定义标签更新成功');
    } catch (error) {
      console.error('更新自定义标签失败:', error);
      message.error('更新自定义标签失败');
      // 更新失败时，重新加载当前标签
      if (values.id) {
        await loadDocTags(values.id);
      }
    } finally {
      setLoadingTopics(false);
    }
  };

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.pdf',
    maxCount: 1,
    showUploadList: true,
    // beforeUpload: (file) => {
    //   // 1. 获取当前表单值
    //   const formValues = formRef.current?.getFieldsValue();
    //   const type = formValues?.type || 1;

    //   // 2. 检查必填字段
    //   // if (!formValues?.title?.trim()) {
    //   //   message.error('请先填写文档名称后保存，再上传文件');
    //   //   return false;
    //   // }

    //   // 如果是图书类型，检查ISBN
    //   // if (type === 1 && !formValues?.isbn?.trim()) {
    //   //   message.error('图书类型必须填写ISBN后保存，再上传文件');
    //   //   return false;
    //   // }

    //   // 3. 检查是否已保存图书信息
    //   // if (!values?.id) {
    //   //   message.error('请先保存图书基本信息，再上传文件');
    //   //   return false;
    //   // }

    //   return true;
    // },
    customRequest: async (options) => {
      const { file, onProgress, onError, onSuccess } = options;
      const formData = new FormData();
      formData.append('file', file);

      try {
        const xhr = new XMLHttpRequest();
        xhr.upload.addEventListener('progress', (event) => {
          if (event.lengthComputable) {
            const percent = Math.round((event.loaded / event.total) * 100);
            onProgress?.({ percent });
          }
        });

        xhr.addEventListener('load', () => {
          if (xhr.status === 200) {
            try {
              const response = JSON.parse(xhr.responseText);
              if (response.code === 0) {
                message.success('文件上传成功');
                onSuccess?.(response.data);
                formRef.current?.setFieldValue('fileId', response.data);
              } else {
                message.error(response.message || '上传失败');
                onError?.(new Error(response.message));
              }
            } catch (e) {
              message.error('上传失败');
              onError?.(new Error('解析响应失败'));
            }
          } else if (xhr.status === 413) {
            message.error('文件过大，单个文件最大支持1000MB');
            onError?.(new Error('文件过大'));
          } else {
            message.error('上传失败');
            onError?.(new Error('上传失败'));
          }
        });

        xhr.addEventListener('error', () => {
          message.error('文件上传失败');
          onError?.(new Error('上传失败'));
        });

        // const docId = values?.id;
        // xhr.open('POST', `/api/doc/upload/${docId}`, true);
        xhr.open('POST', `/api/doc/upload`, true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
        const token = localStorage.getItem('token');
        if (token) {
          xhr.setRequestHeader('token', token);
        }
        xhr.send(formData);
      } catch (err) {
        message.error('文件上传失败');
        onError?.(err as Error);
      }
    },
    onChange: (info) => {
      const { status, name } = info.file;
      if (status === 'done') {
        message.success(`${name} 文件上传成功，请点击保存更新图书信息`);
      } else if (status === 'error') {
        message.error(`${name} 文件上传失败`);
      }
    },
  };

  // 添加处理表单提交的函数
  const handleSubmit = async (formValues: DocItem) => {
    try {
      const processedValues: DocItem = {
        ...formValues,
      };

      if (values?.id) {
        processedValues.id = values.id;
      }
      
      // 处理数字类型字段
      const numberFields = ['pageSize', 'type', 'status', 'score'] as const;
      numberFields.forEach(field => {
        const value = formValues[field];
        if (value !== undefined && value !== null && value.toString().trim() !== '') {
          processedValues[field] = typeof value === 'string' ? parseInt(value, 10) : value;
        } else {
          delete processedValues[field];
        }
      });

      // 处理可选字符串字段
      const stringFields = [
        'subTitle', 'author', 'publisher', 'publicationYear', 
        'publicationDate', 'category', 'keyWord', 'summary',
        'cn', 'series', 'source', 'note'
      ] as const;
      stringFields.forEach(field => {
        const value = formValues[field];
        if (!value && typeof value !== 'number') {
          delete processedValues[field];
        }
      });

      // 检查文件ID是否与原值相同，如果相同则不提交文件ID
      // if (values?.fileId === processedValues.fileId) {
      //   delete processedValues.fileId;
      // }

      const result = await onFinish(processedValues);
      if (result) {
        message.success(values?.id ? '更新成功' : '添加成功');
      }
      return result;
    } catch (error: any) {
      message.error('操作失败：' + (error.message || '未知错误'));
      return false;
    }
  };

  return (
    <ModalForm
      title={values?.id ? '编辑图书' : '新增图书'}
      width={1000}
      visible={visible}
      onVisibleChange={onVisibleChange}
      onFinish={handleSubmit}
      formRef={formRef}
      initialValues={values}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
      validateMessages={{
        required: '${label}不能为空',
        types: {
          number: '${label}必须是数字',
        },
      }}
    >

      <Row gutter={24}>
        <Col span={12}>
          {/* 基本信息 */}
          <ProFormText
            name="title"
            label="文档名称"
            rules={[
              { required: true, message: '请输入文档名称' },
              {
                validator: async (_, value) => {
                  if (value && (value.includes(':') || value.includes('：'))) {
                    return Promise.reject(new Error('文档名称不得包含冒号，请用空格代替'));
                  }
                },
              },
            ]}
          />
          <ProFormText
            name="subTitle"
            label="副标题"
          />
          <ProFormText
            name="author"
            label="作者"
          />
          <ProFormSelect
            name="type"
            label="文档类型"
            // 默认值
            initialValue={1}
            rules={[{ required: true, message: '请选择文档类型' }]}
            options={[
              { label: '图书', value: 1 },
              { label: '自定义PDF', value: 2 },
            ]}
          />
          <ProFormText
            name="isbn"
            label="ISBN"
            dependencies={['type']}
            rules={[
              ({ getFieldValue }: { getFieldValue: (field: string) => any }) => ({
                required: getFieldValue('type') === 1,
                message: '请输入ISBN'
              })
            ]}
          />
          <Form.Item
            name="category"
            label="分类"
          >
            <Select
              showSearch
              allowClear
              placeholder="请选择或输入分类"
              options={categories}
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              mode="tags"
              maxTagCount={1}
              onSelect={(value) => {
                formRef.current?.setFieldValue('category', value);
              }}
            />
          </Form.Item>
          <ProFormSelect
            name="status"
            label="状态"
            options={[
              { label: '正常', value: 0 },
              { label: '禁用', value: 1 },
            ]}
          />
          <ProFormDigit
            name="pageSize"
            label="页数"
            min={0}
            fieldProps={{
              precision: 0,
            }}
          />
        </Col>
        <Col span={12}>
          {/* 出版信息和其他 */}
          <ProFormText
            name="publisher"
            label="出版社"
          />
          <ProFormText
            name="publicationYear"
            label="出版年份"
          />
          <ProFormText
            name="publicationDate"
            label="出版日期"
          />
          <ProFormText
            name="keyWord"
            label="关键词"
          />
          <ProFormText
            name="cn"
            label="中图分类号"
          />
          <ProFormText
            name="series"
            label="丛编"
          />
          <ProFormText
            name="source"
            label="来源"
          />
        </Col>
      </Row>

      {/* 主题标签选择 */}
      <Row gutter={24}>
        <Col span={12}>
          <Form.Item label="主题标签" extra="可多选，支持搜索">
            <TreeSelect
              treeData={topicTreeData}
              value={selectedTopics}
              onChange={handleTopicChange}
              treeCheckable
              showCheckedStrategy={TreeSelect.SHOW_PARENT}
              placeholder="请选择或搜索主题标签"
              style={{ width: '100%' }}
              loading={loadingTopics}
              allowClear
              maxTagCount={5}
              treeNodeFilterProp="title"
              showSearch
              filterTreeNode={(input, node) => {
                return (node?.title as string)?.toLowerCase().indexOf(input.toLowerCase()) >= 0;
              }}
              virtual={false}
              listHeight={256}
              treeDefaultExpandAll={false}
              dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
              treeNodeLabelProp="title"
            />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item label="自定义标签" extra="可多选，支持搜索">
            <Select
              mode="multiple"
              style={{ width: '100%' }}
              placeholder="请选择或搜索自定义标签"
              loading={loadingTopics}
              allowClear
              maxTagCount={5}
              showSearch
              filterOption={(input, option) =>
                (option?.label as string)?.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }
              value={selectedTags}
              onChange={handleTagChange}
              options={tagOptions}
            />
          </Form.Item>
        </Col>
      </Row>

      {/* 长文本区域 */}
      <Row gutter={24}>
        <Col span={24}>
          <ProFormTextArea
            name="summary"
            label="摘要"
            fieldProps={{
              rows: 4,
            }}
          />
          <ProFormTextArea
            name="note"
            label="备注"
            fieldProps={{
              rows: 4,
            }}
          />
        </Col>
      </Row>

      {/* 图片预览和上传区域 */}
      <Row gutter={24}>
        {/* 文件相关 */}
        <Col span={12}>
          <div style={{ marginTop: 16, height: 199 }}>
            <Upload.Dragger {...uploadProps}>
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">
                {values?.id ? '点击或拖拽新的PDF文件到此区域以替换原文件' : '点击或拖拽文件到此区域上传'}
              </p>
              <p className="ant-upload-hint">
                支持单个PDF文件上传，文件大小请控制在1000MB以内
              </p>
            </Upload.Dragger>
          </div>
          <ProFormText
            name="fileId"
            label="文件id"
            disabled
            placeholder=""
            fieldProps={{
              addonAfter: values?.fileId ? (
                <a
                  onClick={async (e) => {
                    e.preventDefault();
                    if (!values.id) return;
                    try {
                      const response = await getPreviewUrl(values.id);
                      if (response.code === 0 && response.data) {
                        window.open('/api3/'+response.data, '_blank');
                      } else {
                        message.error(response.message || '获取预览地址失败');
                      }
                    } catch (error) {
                      message.error('获取预览地址失败');
                    }
                  }}
                >
                  查看文件
                </a>
              ) : null,
            }}
          />
        </Col>
        <Col span={12}>
          <div style={{marginTop: 14}}>
            <ImageUpload
                  value={coverUrl}
                  onChange={handleCoverChange}
                  maxSize={5}
                />
                <ProFormText
                  name="picUrl"
                  label="封面"
                />
          </div>
        </Col>

      </Row>
    </ModalForm>
  );
};

export default DocForm; 