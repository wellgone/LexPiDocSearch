import React, { useState } from 'react';
import { Upload, message } from 'antd';
import type { UploadProps } from 'antd';
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons';
import './index.css';

interface ImageUploadProps {
  value?: string;
  onChange?: (value: string) => void;
  maxSize?: number; // 最大文件大小，单位MB
}

const ImageUpload: React.FC<ImageUploadProps> = ({ 
  value, 
  onChange, 
  maxSize = 5 // 默认5MB
}) => {
  const [loading, setLoading] = useState(false);

  // 将文件转换为base64
  const getBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = error => reject(error);
    });
  };

  const beforeUpload = (file: File) => {
    // 检查文件类型
    const isImage = file.type.startsWith('image/');
    if (!isImage) {
      message.error('只能上传图片文件！');
      return false;
    }

    // 检查文件大小
    const isLtMaxSize = file.size / 1024 / 1024 < maxSize;
    if (!isLtMaxSize) {
      message.error(`图片必须小于 ${maxSize}MB!`);
      return false;
    }

    return true;
  };

  const handleChange: UploadProps['onChange'] = async (info) => {
    if (info.file.status === 'uploading') {
      setLoading(true);
      return;
    }

    if (info.file.status === 'done') {
      try {
        // 获取文件对象
        const file = info.file.originFileObj;
        if (!file) return;

        // 转换为base64
        const base64Data = await getBase64(file);
        
        // 调用onChange回调
        onChange?.(base64Data);
        setLoading(false);
      } catch (error) {
        console.error('转换图片失败:', error);
        message.error('图片处理失败');
        setLoading(false);
      }
    }
  };

  const uploadButton = (
    <div className="upload-placeholder" >
      {loading ? <LoadingOutlined /> : <PlusOutlined />}
      <div className="upload-text">上传图片</div>
    </div>
  );

  return (
    <Upload
      name="file"
      listType="picture-card"
      className="imageUploader"
      showUploadList={false}
      beforeUpload={beforeUpload}
      onChange={handleChange}
      customRequest={({ onSuccess }) => {
        // 使用自定义上传，避免自动发送请求
        if (onSuccess) onSuccess({});
      }}
    >
      {value ? (
        <img src={value} alt="封面" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
      ) : (
        uploadButton
      )}
    </Upload>
  );
};

export default ImageUpload; 