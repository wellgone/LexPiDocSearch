import { PageContainer } from '@ant-design/pro-components';
import { Card, theme, Tag, Space } from 'antd';
import {
  BookOutlined,
  FileSearchOutlined,
  TagsOutlined,
  RobotOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  PlusOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  TagOutlined,
  SearchOutlined,
  BulbOutlined,
} from '@ant-design/icons';
import React from 'react';
import AIAssistant from '@/components/AIAssistant';

/**
 * 每个单独的卡片，为了复用样式抽成了组件
 * @param param0
 * @returns
 */
const InfoCard: React.FC<{
  title: string;
  index: number;
  desc: string;
  href: string;
}> = ({ title, href, index, desc }) => {
  const { useToken } = theme;
  const { token } = useToken();

  return (
    <div
      style={{
        backgroundColor: token.colorBgContainer,
        boxShadow: token.boxShadow,
        borderRadius: '8px',
        fontSize: '14px',
        color: token.colorTextSecondary,
        lineHeight: '22px',
        padding: '16px 19px',
        minWidth: '220px',
        flex: 1,
      }}
    >
      <div
        style={{
          display: 'flex',
          gap: '4px',
          alignItems: 'center',
        }}
      >
        <div
          style={{
            width: 48,
            height: 48,
            lineHeight: '22px',
            backgroundSize: '100%',
            textAlign: 'center',
            padding: '8px 16px 16px 12px',
            color: '#FFF',
            fontWeight: 'bold',
            backgroundImage:
              "url('https://gw.alipayobjects.com/zos/bmw-prod/daaf8d50-8e6d-4251-905d-676a24ddfa12.svg')",
          }}
        >
          {index}
        </div>
        <div
          style={{
            fontSize: '16px',
            color: token.colorText,
            paddingBottom: 8,
          }}
        >
          {title}
        </div>
      </div>
      <div
        style={{
          fontSize: '14px',
          color: token.colorTextSecondary,
          textAlign: 'justify',
          lineHeight: '22px',
          marginBottom: 8,
        }}
      >
        {desc}
      </div>
      <a href={href} target="_blank" rel="noreferrer">
        了解更多 {'>'}
      </a>
    </div>
  );
};

const Welcome: React.FC = () => {
  const { token } = theme.useToken();

  return (
    <PageContainer>
      <Card
        style={{
          borderRadius: 16,
          boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
        }}
        bodyStyle={{
          backgroundImage: 'linear-gradient(135deg, #FBFDFF 0%, #F5F7FF 100%)',
          padding: '40px',
        }}
      >
        <div
          style={{
            backgroundPosition: '100% -30%',
            backgroundRepeat: 'no-repeat',
            backgroundSize: '300px auto',
            backgroundImage:
              "url('https://gw.alipayobjects.com/mdn/rms_a9745b/afts/img/A*BuFmQqsB2iAAAAAAAAAAAAAAARQnAQ')",
          }}
        >
          <div
            style={{
              fontSize: '28px',
              fontWeight: 600,
              color: token.colorTextHeading,
              marginBottom: '24px',
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
            }}
          >
            <BookOutlined style={{ fontSize: '32px', color: '#1890ff' }} />
            欢迎使用律π知识管理系统
            <Space style={{ marginLeft: '16px' }}>
              <Tag color="blue" icon={<FileSearchOutlined />}>智能检索</Tag>
              <Tag color="green" icon={<TagsOutlined />}>标签管理</Tag>
              <Tag color="purple" icon={<RobotOutlined />}>AI助手</Tag>
            </Space>
          </div>
          <div
            style={{
              fontSize: '15px',
              color: token.colorTextSecondary,
              lineHeight: '1.8',
              marginBottom: '40px',
              width: '100%',
              background: 'rgb(255, 255, 255)',
              padding: '24px',
              borderRadius: '12px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
            }}
          >
            <div style={{ marginBottom: '16px' }}>
              <Space>
                <BulbOutlined style={{ color: '#1890ff', fontSize: '18px' }} />
                这是一个智能化的知识管理平台。
              </Space>
              <div style={{
                background: '#FFF3F0',
                padding: '12px 16px',
                borderRadius: '8px',
                marginTop: '12px',
                border: '1px solid #FFCCC7',
                color: '#CF1322',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                width: 'fit-content', //  调整宽度为按内容自适应
              }}>
                <WarningOutlined />
                <strong>注意：全文检索基于OCR处理后的文本层，PDF文档导入系统前需经过OCR处理。</strong>
                <br />
              </div>
              
            </div>
            
            <div style={{ marginBottom: '16px' }}>
              <div style={{ 
                fontWeight: 500, 
                marginBottom: '16px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '16px',
                color: token.colorTextHeading,
              }}>
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
                请按以下步骤操作：
              </div>
              <div style={{ paddingLeft: '16px' }}>
                <div style={{ marginBottom: '16px' }}>
                  <div style={{ 
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    color: token.colorTextHeading,
                  }}>
                    <PlusOutlined style={{ color: '#1890ff' }} />
                    1. 导入文档
                    <Tag color="blue">支持批量</Tag>
                  </div>
                  <div style={{ paddingLeft: '24px', color: token.colorTextSecondary }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
                      <CloudUploadOutlined /> 单文导入：点击&ldquo;新增&rdquo;按钮，填写基本信息并上传文档（注意，上传文档前请先保存基本信息）
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
                      <DatabaseOutlined /> 批量导入：点击&ldquo;批量导入&rdquo;按钮，使用模板导入多个文档
                    </div>
                  </div>
                </div>
                <div style={{ marginBottom: '16px' }}>
                  <div style={{ 
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    color: token.colorTextHeading,
                  }}>
                    <DatabaseOutlined style={{ color: '#1890ff' }} />
                    2. 获取OPAC数据
                    <Tag color="orange">可选</Tag>
                  </div>
                  <div style={{ paddingLeft: '24px', color: token.colorTextSecondary }}>
                    点击&ldquo;获取OPAC&rdquo;按钮，自动获取书籍基本信息
                  </div>
                </div>
                <div style={{ marginBottom: '16px' }}>
                  <div style={{ 
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    color: token.colorTextHeading,
                  }}>
                    <TagOutlined style={{ color: '#1890ff' }} />
                    3. 添加标签分类
                    <Tag color="green">推荐</Tag>
                  </div>
                  <div style={{ paddingLeft: '24px', color: token.colorTextSecondary }}>
                    建议在建立索引前完成，方便后续检索过滤
                  </div>
                </div>
                <div style={{ marginBottom: '16px' }}>
                  <div style={{ 
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    color: token.colorTextHeading,
                  }}>
                    <SearchOutlined style={{ color: '#1890ff' }} />
                    4. 抽取内容并建立索引
                    <Tag color="purple">自动处理</Tag>
                  </div>
                  <div style={{ paddingLeft: '24px', color: token.colorTextSecondary }}>
                    点击&ldquo;抽取&rdquo;和&ldquo;索引&rdquo;按钮（批量导入可使用&ldquo;提取&索引&rdquo;按钮）
                  </div>
                </div>
                <div>
                  <div style={{ 
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    color: token.colorTextHeading,
                  }}>
                    <FileSearchOutlined style={{ color: '#1890ff' }} />
                    5. 使用全文检索功能
                    <Tag color="cyan">智能搜索</Tag>
                  </div>
                </div>
              </div>
            </div>
            
            <div style={{ 
              marginTop: '24px',
              padding: '12px 16px',
              background: '#F6FFED',
              borderRadius: '8px',
              border: '1px solid #B7EB8F',
              color: '#389E0D',
              display: 'flex',
              alignItems: 'center',
              width: 'fit-content', //  调整宽度为按内容自适应
              gap: '8px',
            }}>
              <RobotOutlined />
              如需帮助，可随时点击右下角的AI助手图标获取支持。
            </div>
          </div>
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: 24,
            }}
          >
            <InfoCard
              index={1}
              href="/doc/list"
              title="文档管理"
              desc="第一步：在文档管理页面中导入您的图书信息。支持单本添加或批量导入，一键获取OPAC数据，丰富图书元数据。"
            />
            <InfoCard
              index={2}
              title="标签管理"
              href="/doc/tags"
              desc="第二步：为已导入的图书添加标签，进行分类整理。这将帮助您更好地组织文档，也能提升后续检索的精确度。"
            />
            <InfoCard
              index={3}
              title="全文检索"
              href="/doc/fullTextSearch"
              desc="第三步：完成以上步骤后，系统会自动建立索引。您可以使用全文检索功能，支持多字段组合查询、同义词扩展等高级特性。"
            />
          </div>
        </div>
      </Card>
      <AIAssistant />
    </PageContainer>
  );
};

export default Welcome;
