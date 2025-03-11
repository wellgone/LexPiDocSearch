import { ModalForm, ProFormText } from '@ant-design/pro-components';
import type { TagItem } from '../data.d';
import { useEffect, useState, useRef } from 'react';
import { Select, Divider, Input, Space, Button, InputRef } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

interface TagFormProps {
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onFinish: (values: TagItem) => Promise<boolean>;
  values?: TagItem;
  tagTypes: any[];
}

const TagForm: React.FC<TagFormProps> = (props) => {
  const { visible, onVisibleChange, onFinish, values, tagTypes } = props;
  const [initialValues, setInitialValues] = useState<any>({});
  const [localTagTypes, setLocalTagTypes] = useState<any[]>(tagTypes);
  const [newTypeName, setNewTypeName] = useState('');
  const [selectedType, setSelectedType] = useState<string | undefined>(undefined);
  const inputRef = useRef<InputRef>(null);

  // 同步外部tagTypes到本地
  useEffect(() => {
    setLocalTagTypes(tagTypes);
  }, [tagTypes]);

  // 监听values和tagTypes的变化，更新表单初始值
  useEffect(() => {
    if (values && localTagTypes.length > 0) {
      const newInitialValues = {
        id: values.id,
        name: values.name,
      };
      setSelectedType(values.type || localTagTypes[0]?.value);
      setInitialValues(newInitialValues);
    } else {
      setInitialValues({});
      setSelectedType(localTagTypes[0]?.value);
    }
  }, [values, localTagTypes]);

  const onTypeNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setNewTypeName(event.target.value);
  };

  const addType = (e: React.MouseEvent<HTMLButtonElement | HTMLAnchorElement>) => {
    e.preventDefault();
    if (!newTypeName.trim()) return;

    // 获取当前最大level
    const maxLevel = Math.max(...localTagTypes.map(t => t.level));
    const newType = {
      label: newTypeName.trim(),
      value: newTypeName.trim(),
      level: maxLevel + 1,
    };

    setLocalTagTypes([...localTagTypes, newType]);
    // 自动选中新添加的类型
    setSelectedType(newType.value);
    setNewTypeName('');
    setTimeout(() => {
      inputRef.current?.focus();
    }, 0);
  };

  // 获取新类型的level值
  const getNewTypeLevel = () => {
    const maxLevel = Math.max(...localTagTypes.map(t => t.level));
    return maxLevel + 1;
  };

  return (
    <ModalForm
      title={values ? '编辑标签' : '新建标签'}
      width={500}
      open={visible}
      onVisibleChange={onVisibleChange}
      onFinish={async (formValues: TagItem) => {
        // 查找选中的类型是否存在于原始类型列表中
        const existingType = tagTypes.find(t => t.value === selectedType);
        const typeInfo = localTagTypes.find(t => t.value === selectedType);

        const success = await onFinish({
          ...formValues,
          type: selectedType,
          // 如果是新类型（不在原始类型列表中），使用新计算的level值
          level: existingType?.level || typeInfo?.level || getNewTypeLevel(),
        });

        if (success) {
          onVisibleChange(false);
        }
        return success;
      }}
      initialValues={initialValues}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
    >
      <ProFormText
        name="name"
        label="标签名称"
        placeholder="请输入标签名称"
        rules={[{ required: true, message: '请输入标签名称' }]}
      />
      <div className="ant-form-item">
        <div className="ant-form-item-label">
          <label className="ant-form-item-required">标签类型</label>
        </div>
        <div className="ant-form-item-control">
          <div className="ant-form-item-control-input">
            <div className="ant-form-item-control-input-content">
              <Select
                style={{ width: '100%' }}
                value={selectedType}
                onChange={(value) => setSelectedType(value)}
                placeholder="请选择或输入标签类型"
                dropdownRender={(menu) => (
                  <>
                    {menu}
                    <Divider style={{ margin: '8px 0' }} />
                    <Space style={{ padding: '0 8px 4px' }}>
                      <Input
                        placeholder="请输入新的标签类型"
                        ref={inputRef}
                        value={newTypeName}
                        onChange={onTypeNameChange}
                        onKeyDown={(e) => e.stopPropagation()}
                      />
                      <Button type="text" icon={<PlusOutlined />} onClick={addType}>
                        添加类型
                      </Button>
                    </Space>
                  </>
                )}
                options={localTagTypes}
                showSearch
                allowClear
                filterOption={(input: string, option: any) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
              />
            </div>
          </div>
        </div>
      </div>
    </ModalForm>
  );
};

export default TagForm; 