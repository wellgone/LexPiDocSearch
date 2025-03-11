import { useCurrentRefinements } from 'react-instantsearch';
import { Tag, Space, Typography } from 'antd';
import { CloseOutlined } from '@ant-design/icons';
import { useMemo, useState } from 'react';

const { Text } = Typography;


// 文档类型选项
const typeOptions = [
  { value: 1, label: '图书' },
  { value: 2, label: '自定义PDF' },
];

const CustomCurrentRefinements = (props: any) => {
    const { items, refine } = useCurrentRefinements(props);

    const getLabelInChinese = (label: string) => {
      const labels: { [key: string]: string } = {
          'book_title': '书名',
          'author': '作者',
          'publisher': '出版社',
          'publication_year': '年份',
          'topics_lvl0': '主题',
          'topics_lvl1': '主题',
          'topics_lvl2': '主题',
          'topics_lvl3': '主题',
          'topics_lvl4': '主题',
          'tags': '标签',
          'opac_series': 'OPAC系列',
          'series': '系列',
          'type': '文档类型'
      };
      return labels[label] || label; // 如果没有匹配，返回原始标签
  };
  
    if (items.length === 0) {
      return null;
    }
  
    return (
      <div style={{ padding: '8px 8px 0 6px', maxHeight: 100, overflowY: 'auto' }}>
        <Space size={[0, 4]} wrap>
          <span style={{ marginRight: 4 }}>当前筛选：</span>
          {items.map((item) => (
            item.refinements.map((refinement) => {
                return (
                    <Tag
                        key={[item.label, refinement.label].join('/')}
                        color="processing"
                        closable
                        closeIcon={<CloseOutlined />}
                        onClose={(e) => {
                            e.preventDefault();
                            refine(refinement);
                            }}
                        style={{ 
                        marginRight: 8,
                        marginBottom: 0,
                        padding: '1px 3px',
                        fontSize: '12px'
                        }}
                    >
                        {getLabelInChinese(item.label)}: {getLabelInChinese(item.label) === '文档类型' ? typeOptions.find(option => String(option.value) === String(refinement.label))?.label || refinement.label : refinement.label}
                    </Tag>
                )
            })
          ))}
        </Space>
      </div>
    );
  };

export default CustomCurrentRefinements;