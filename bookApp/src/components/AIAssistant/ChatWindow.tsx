import React, { useState } from 'react';
import { Bubble, Sender } from '@ant-design/x';
import { UserOutlined, RobotOutlined } from '@ant-design/icons';
import { Avatar } from 'antd';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import styles from './ChatWindow.module.less';

interface Message {
  type: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

interface ChatWindowProps {
  messages: Message[];
  loading?: boolean;
  onSend?: (content: string) => void;
}

const ChatWindow: React.FC<ChatWindowProps> = ({ messages, loading, onSend }) => {
  const [inputValue, setInputValue] = useState('');

  const handleSubmit = (value: string) => {
    if (value.trim()) {
      onSend?.(value);
      setInputValue('');
    }
  };

  const handleChange = (value: string) => {
    setInputValue(value);
  };

  const renderContent = (content: string) => {
    if (content.includes('```') || content.includes('**') || content.includes('#')) {
      return (
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          rehypePlugins={[rehypeRaw]}
          className={styles.markdown}
        >
          {content}
        </ReactMarkdown>
      );
    }
    return content;
  };

  return (
    <div className={styles.chatWindow}>
      <div className={styles.messageList}>
        <Bubble.List
          items={messages.map((msg) => ({
            content: renderContent(msg.content),
            placement: msg.type === 'user' ? 'end' : 'start',
            loading: msg.type === 'assistant' && loading && msg === messages[messages.length - 1],
            avatar: (
              <Avatar
                icon={msg.type === 'user' ? <UserOutlined /> : <RobotOutlined />}
                style={{ backgroundColor: msg.type === 'user' ? '#1890ff' : '#52c41a' }}
              />
            ),
            footer: new Date(msg.timestamp).toLocaleString(),
          }))}
        />
      </div>
      <div className={styles.inputArea}>
        <Sender
          value={inputValue}
          onChange={handleChange}
          placeholder="输入您的问题..."
          disabled={loading}
          onSubmit={handleSubmit}
        />
      </div>
    </div>
  );
};

export default ChatWindow; 