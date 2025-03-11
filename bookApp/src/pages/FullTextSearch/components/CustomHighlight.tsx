import React from 'react';
import { Typography } from 'antd';

const { Text } = Typography;

interface CustomHighlightProps {
  attribute: string;
  hit: any;
  highlightedTagName?: string;
  className?: string;
  style?: React.CSSProperties;
}

// 获取对象的嵌套属性值
const getPropertyByPath = (object: any, path: string) => {
  const parts = path.split('.');
  let result = object;
  
  for (const part of parts) {
    if (result && typeof result === 'object') {
      result = result[part];
    } else {
      return undefined;
    }
  }
  return result;
};

const CustomHighlight: React.FC<CustomHighlightProps> = ({
  attribute,
  hit,
  highlightedTagName = 'mark',
  className,
  style
}) => {
  // 获取高亮结果
  const highlightResult = getPropertyByPath(hit._highlightResult, attribute);
  if (!highlightResult) {
    // 如果没有高亮结果，返回原始文本
    const originalText = getPropertyByPath(hit, attribute);
    return originalText ? <Text>{originalText}</Text> : null;
  }

  // 获取高亮文本
  const { value = '' } = highlightResult;

  // 处理高亮标签
  const processHighlight = (text: string) => {
    return {
      __html: attribute === 'book_title' ? 
        text.replace(/<em>/g, `<${highlightedTagName} class="custom-highlight">`)
            .replace(/<\/em>/g, `</${highlightedTagName}>`) :
        '...' + text.replace(/<em>/g, `<${highlightedTagName} class="custom-highlight">`)
            .replace(/<\/em>/g, `</${highlightedTagName}>`) + '...'
    };
  };

  // 如果是书籍标题，直接返回单个标题
  if (attribute === 'book_title') {
    return (
      <h4
        className={className}
        style={{
          margin: 0,
          fontSize: '16px',
          fontWeight: 'bold',
          ...style
        }}
        dangerouslySetInnerHTML={processHighlight(value)}
      />
    );
  }

  // 处理正文内容
  const segments = value.split('@=||=@').filter(Boolean);

  // 如果只有一个片段且没有高亮标记，直接返回文本
  if (segments.length === 1 && !segments[0].includes(highlightedTagName)) {
    return <Text>{segments[0]}</Text>;
  }

  return (
    <ul className={`highlight-list ${className || ''}`} style={style}>
      {segments.map((segment: string, index: number) => (
        <li key={index} className="highlight-segment">
          <Text>
            <div
              dangerouslySetInnerHTML={processHighlight(segment)}
              style={{
                fontSize: '14px',
                lineHeight: '1.5',
                color: 'rgba(0, 0, 0, 0.85)',
                padding: '1px 0'
              }}
            />
          </Text>
        </li>
      ))}
    </ul>
  );
};

export default CustomHighlight; 