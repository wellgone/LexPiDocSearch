import { Modal, Form, Input, Select, Space, Tag } from 'antd';
import { useEffect, useState } from 'react';
import React from 'react';
import type { TopicItem } from '../data.d';
import request from '@/utils/request';

interface TopicFormProps {
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onFinish: (values: TopicItem) => Promise<boolean>;
  values?: TopicItem;
}

const TopicForm: React.FC<TopicFormProps> = ({
  visible,
  onVisibleChange,
  onFinish,
  values,
}) => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [parentTopics, setParentTopics] = useState<TopicItem[]>([]);

  // 获取层级对应的Tag颜色
  const getLevelTagColor = (level: number) => {
    const colors = ['blue', 'green', 'gold', 'orange', 'volcano', 'purple'];
    return level === 0 ? 'blue' : colors[level % colors.length];
  };

  const fetchParentTopics = async () => {
    try {
      const response = await request('/api/topic/list', {
        method: 'GET',
      });
      if (response.code === 0) {
        let filteredTopics = response.data.filter((topic: TopicItem) => topic.id !== values?.id);
        // 在最前面插入一个选项，表示创建根节点
        filteredTopics.unshift({ id: '0', name: '创建根节点', level: 0 });
        setParentTopics(filteredTopics);
      }
    } catch (error) {
      console.error('加载父级标签失败:', error);
    }
  };

  useEffect(() => {
    if (visible) {
      form.resetFields();
      if (values) {
        form.setFieldsValue(values);
      }
      fetchParentTopics();
    }
  }, [visible, values, form]);

  const handleSubmit = async () => {
    try {
      const fieldsValue = await form.validateFields();
      // 自动计算层级：如果选择"创建根节点"，则level为0，否则为父级标签层级 + 1
      const parentTopic = parentTopics.find(topic => topic.id === fieldsValue.parentId);
      fieldsValue.level = parentTopic?.id === '0' ? 0 : (parentTopic?.level ?? 0) + 1;
      // 如果选择"创建根节点"，将parentId设置为0
      fieldsValue.parentId = fieldsValue.parentId === '0' ? '0' : fieldsValue.parentId;
      
      setSubmitting(true);
      const success = await onFinish(fieldsValue);
      if (success) {
        onVisibleChange(false);
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    } finally {
      setSubmitting(false);
    }
  };

  // 自定义选项渲染
  const renderOption = (topic: TopicItem) => {
    const level = topic.level ?? 0;
    return {
      label: (
        <Space>
          <span>{topic.name}</span>
          {topic.id !== '0' && (
            <Tag color={getLevelTagColor(level)} style={{ minWidth: '60px', textAlign: 'center' }}>
              {level === 0 ? '根节点' : `第${level}级`}
            </Tag>
          )}
        </Space>
      ),
      value: topic.id,
    };
  };

  // 自定义搜索过滤函数
  const filterOption = (input: string, option: any) => {
    const label = option?.label;
    if (React.isValidElement(label)) {
      const spaceElement = label as React.ReactElement<{ children: React.ReactNode[] }>;
      const spanElement = spaceElement.props.children[0] as React.ReactElement;
      return spanElement.props.children.toLowerCase().includes(input.toLowerCase());
    }
    return false;
  };

  return (
    <Modal
      title={values ? '编辑标签' : '新增标签'}
      open={visible}
      onOk={handleSubmit}
      onCancel={() => onVisibleChange(false)}
      confirmLoading={submitting}
      width={500}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          parentId: '0',
        }}
      >
        <Form.Item
          name="name"
          label="标签名称"
          rules={[{ required: true, message: '请输入标签名称' }]}
        >
          <Input placeholder="请输入标签名称" />
        </Form.Item>

        <Form.Item
          name="parentId"
          label="父级标签"
          rules={[{ 
            required: true, 
            message: '请选择父级标签',
            validator: (_, value) => {
              if (value === undefined || value === null) {
                return Promise.reject(new Error('请选择父级标签'));
              }
              return Promise.resolve();
            }
          }]}
        >
          <Select
            showSearch
            placeholder="请输入关键字搜索或选择父级标签"
            optionFilterProp="label"
            filterOption={filterOption}
            options={parentTopics.map(renderOption)}
            style={{ width: '100%' }}
            optionLabelProp="label"
            dropdownStyle={{ minWidth: '300px' }}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default TopicForm; 