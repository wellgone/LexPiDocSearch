import React from 'react';
import { useHits, Highlight } from 'react-instantsearch';
import { List, Tag, Typography, Space, Tooltip } from 'antd';
import { FileTextOutlined, BookOutlined, FieldTimeOutlined } from '@ant-design/icons';
import CustomHighlight from './CustomHighlight';
import './CustomHits.css';

const { Text, Title } = Typography;

interface CustomHitsProps {
  onHitClick: (hit: any) => void;
  currentKeyWord: string;
}

const CustomHits: React.FC<CustomHitsProps> = ({ onHitClick, currentKeyWord }) => {
  const { 
    items,
    results,
  } = useHits();

  console.log("items",items)
  console.log("results",results)
//   console.log("banner",banner)
//   console.log("sendEvent",sendEvent)

  return (
    <List
      itemLayout="vertical"
      dataSource={items}
      renderItem={(hit: any) => (
        <List.Item
          key={hit.objectID}
          onClick={() => onHitClick(hit)}
          className="custom-hit-item"
          style={{ cursor: 'pointer' }}
        >
          <List.Item.Meta
            // avatar={<BookOutlined style={{ fontSize: '24px', color: '#1890ff' }} />}
            style={{ marginBottom: '0px' }}
            title={
              <Space size={1}>
                <span style={{ margin: 0,fontSize: '16px',fontWeight: 'bold' }}>
                  <CustomHighlight attribute="book_title" hit={hit} />
                </span>
                <Tag color="blue">P{hit.page_num}</Tag>
                <Tag color="processing">{hit._score.toFixed(2)}</Tag>
              </Space>
            }
            description={hit.author || hit.publication_year || hit.publisher ? (
              <Space>
                {hit.author && (
                  <Tooltip title="作者">
                    <Text type="secondary">
                      <FileTextOutlined /> {hit.author}
                    </Text>
                  </Tooltip>
                )}
                {hit.publication_year && (
                  <Tooltip title="出版年份">
                    <Text type="secondary">
                    <FieldTimeOutlined /> {hit.publication_year}
                    </Text>
                  </Tooltip>
                )}
                {hit.publisher && (
                  <Tooltip title="出版社">
                    <Text type="secondary">
                      <BookOutlined /> {hit.publisher}
                    </Text>
                  </Tooltip>
                )}
              </Space>
            ) : null}
          />
          <div className="hit-content" style={{
            maxHeight: currentKeyWord === '' ? '110px' : 'none',
            overflow: currentKeyWord === '' ? 'hidden' : 'visible',
            textOverflow: currentKeyWord === '' ? 'ellipsis' : 'clip',
            WebkitBoxOrient: 'vertical',
            WebkitLineClamp: currentKeyWord === '' ? 5 : 'none', }}
            >
            <CustomHighlight
              attribute="section_text"
              hit={hit}
              className="hit-snippet"
              style={{
                fontSize: '14px',
                lineHeight: 1.5,
                color: 'rgba(0, 0, 0, 0.65)',
                ...(currentKeyWord === '' && {
                  maxHeight: '150px',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  WebkitBoxOrient: 'vertical',
                  WebkitLineClamp: 5,
                }),
              }}
            />
          </div>
        </List.Item>
      )}
    />
  );
};

export default CustomHits; 