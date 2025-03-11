import { Modal, Input, Select, message } from 'antd';
import { useState } from 'react';
import request from '@/utils/request';

const { TextArea } = Input;

interface TagImportModalProps {
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onSuccess: () => void;
  tagTypes: any[];
}

const TagImportModal: React.FC<TagImportModalProps> = ({
  visible,
  onVisibleChange,
  onSuccess,
  tagTypes,
}) => {
  const [tags, setTags] = useState<string>('');
  const [type, setType] = useState<string>(tagTypes[0]?.value || '通用标签');

  const handleOk = async () => {
    if (!tags.trim()) {
      message.error('请输入标签');
      return;
    }

    const tagList = tags
      .split(/[,，\n]/)
      .map((tag) => tag.trim())
      .filter((tag) => tag);

    try {
      const selectedType = tagTypes.find((t) => t.value === type);
      if (!selectedType) {
        message.error('标签类型无效');
        return;
      }

      await request('/api/topic/tag/import', {
        method: 'POST',
        data: tagList,
        params: {
          level: selectedType.level,
          type: selectedType.value,
        },
      });
      message.success('导入成功');
      setTags('');
      onVisibleChange(false);
      onSuccess();
    } catch (error) {
      message.error('导入失败');
    }
  };

  return (
    <Modal
      title="批量导入标签"
      open={visible}
      onOk={handleOk}
      onCancel={() => {
        setTags('');
        onVisibleChange(false);
      }}
      width={600}
    >
      <div className="mb-4">
        <div className="mb-2">标签类型：</div>
        <Select
          value={type}
          onChange={setType}
          style={{ width: '100%' }}
          options={tagTypes}
        />
      </div>
      <div>
        <div className="mb-2">标签列表：</div>
        <TextArea
          value={tags}
          onChange={(e) => setTags(e.target.value)}
          placeholder="请输入标签，多个标签用逗号、换行分隔"
          rows={10}
        />
      </div>
    </Modal>
  );
};

export default TagImportModal; 