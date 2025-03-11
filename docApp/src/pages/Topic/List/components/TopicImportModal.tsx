import { Modal, Input, message } from 'antd';
import { useState } from 'react';
import request from '@/utils/request';

interface TopicImportModalProps {
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onSuccess: () => void;
}

const TopicImportModal: React.FC<TopicImportModalProps> = ({
  visible,
  onVisibleChange,
  onSuccess,
}) => {
  const [jsonContent, setJsonContent] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);

  const handleSubmit = async () => {
    if (!jsonContent.trim()) {
      message.error('请输入JSON内容');
      return;
    }

    try {
      setSubmitting(true);
      const response = await request('/api/topic/import', {
        method: 'POST',
        data: jsonContent,
      });

      if (response.code === 0) {
        message.success('导入成功');
        onVisibleChange(false);
        setJsonContent('');
        onSuccess();
      } else {
        message.error(response.message || '导入失败');
      }
    } catch (error: any) {
      message.error(error.message || '导入失败');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="批量导入标签"
      open={visible}
      onOk={handleSubmit}
      onCancel={() => {
        onVisibleChange(false);
        setJsonContent('');
      }}
      confirmLoading={submitting}
      width={800}
    >
      <div style={{ marginBottom: 16 }}>
        <p>请输入符合格式的JSON字符串，例如：</p>
        <pre style={{ background: '#f5f5f5', padding: 16, borderRadius: 4 }}>
{`{
  "title": "民法",
  "children": [
    {
      "title": "民法典",
      "children": [
        {
          "title": "总则编"
        },
        {
          "title": "物权编"
        }
      ]
    }
  ]
}`}
        </pre>
      </div>
      <Input.TextArea
        value={jsonContent}
        onChange={(e) => setJsonContent(e.target.value)}
        placeholder="请输入JSON内容"
        rows={10}
      />
    </Modal>
  );
};

export default TopicImportModal; 