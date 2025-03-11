import { Avatar, Tag } from 'antd';
import React from 'react';
import moment from 'moment';
import styles from './index.less';

type ArticleListContentProps = {
  data: {
    content?: string;
    opacSummary?: string;
    summary?: string;
    tags?: any[];
  };
};

const ArticleListContent: React.FC<ArticleListContentProps> = ({
  data: { content, opacSummary, summary, tags },
}) => {
  const description = opacSummary || summary || content || '暂无描述';
  const truncatedDescription = description.length > 140 ? description.slice(0, 140) + '...' : description;

  return (
    <div className={styles.listContent}>
      <div className={styles.description} style={{ height: 60, fontSize: 15, color: '#666' }}>
        {truncatedDescription}
        {/* 标签显示在标题上方 */}
        {tags && tags.length > 0 && (
          <div>
            {tags.map((tag) => (
              <Tag key={tag.id} color="blue">
                {tag.name}
              </Tag>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ArticleListContent;
