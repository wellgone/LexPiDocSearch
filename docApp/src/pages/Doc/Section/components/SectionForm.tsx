import { ModalForm, ProFormTextArea, ProFormDigit } from '@ant-design/pro-components';
import type { DocSection } from '../data.d';
import { useEffect, useRef } from 'react';
import { FormInstance } from 'antd';

interface SectionFormProps {
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onFinish: (values: DocSection) => Promise<boolean>;
  values?: Partial<DocSection>;
}

const SectionForm: React.FC<SectionFormProps> = (props) => {
  const { visible, onVisibleChange, onFinish, values } = props;
  const formRef = useRef<FormInstance>();

  useEffect(() => {
    if (visible && values && formRef.current) {
      formRef.current.setFieldsValue(values);
    }
  }, [visible, values]);

  return (
    <ModalForm
      formRef={formRef}
      title={values?.id ? '编辑章节' : '新增章节'}
      width={600}
      visible={visible}
      onVisibleChange={onVisibleChange}
      onFinish={onFinish}
      modalProps={{
        destroyOnClose: true,
        forceRender: true,
      }}
    >
      <ProFormDigit
        name="docId"
        label="图书ID"
        placeholder="请输入图书ID"
        rules={[{ required: true, message: '请输入图书ID' }]}
      />
      <ProFormDigit
        name="pageNum"
        label="页码"
        placeholder="请输入页码"
        rules={[{ required: true, message: '请输入页码' }]}
      />
      <ProFormTextArea
        name="content"
        label="内容"
        placeholder="请输入内容"
        fieldProps={{
          autoSize: { minRows: 5, maxRows: 20 },
        }}
        rules={[{ required: true, message: '请输入内容' }]}
      />
    </ModalForm>
  );
};

export default SectionForm; 