import React, { useState, useEffect } from 'react';
import {
  Attachments,
  Bubble,
  Conversations,
  Prompts,
  Sender,
  Welcome,
} from '@ant-design/x';
import type { ConversationsProps } from '@ant-design/x';
import { createStyles } from 'antd-style';
import {
  RobotOutlined,
  BookOutlined,
  FileTextOutlined,
  QuestionCircleOutlined,
  PlusOutlined,
  PaperClipOutlined,
  CloudUploadOutlined,
  UserOutlined,
  CopyOutlined,
  DeleteOutlined,
  EditOutlined,
} from '@ant-design/icons';
import { Badge, Button, Space, message, Modal, FloatButton, type GetProp, Avatar, Input, Select, Tooltip } from 'antd';
import { streamChatCompletion } from '@/services/deepseek';
import MarkdownIt from 'markdown-it';

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
  typographer: true,
});

const renderTitle = (icon: React.ReactElement, title: string) => (
  <Space align="start">
    {icon}
    <span>{title}</span>
  </Space>
);

const defaultConversationsItems = [
  {
    key: '0',
    label: '新的对话',
  },
];

const useStyle = createStyles(({ token, css }) => {
  return {
    layout: css`
      width: 100%;
      height: 100%;
      display: flex;
      background: ${token.colorBgContainer};
      font-family: ${token.fontFamily};

      .ant-prompts {
        color: ${token.colorText};
      }
    `,
    menu: css`
      background: ${token.colorBgLayout}80;
      width: 280px;
      height: 100%;
      display: flex;
      flex-direction: column;
      border-right: 1px solid ${token.colorBorderSecondary};
    `,
    conversations: css`
      padding: 0 12px;
      flex: 1;
      overflow-y: auto;
    `,
    chat: css`
      height: 100%;
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: ${token.paddingLG}px;
      gap: 16px;
    `,
    messages: css`
      flex: 1;
      overflow-y: auto;
    `,
    placeholder: css`
      padding-top: 32px;
    `,
    sender: css`
      box-shadow: ${token.boxShadow};
    `,
    logo: css`
      display: flex;
      height: 72px;
      align-items: center;
      justify-content: start;
      padding: 0 24px;
      box-sizing: border-box;
      border-bottom: 1px solid ${token.colorBorderSecondary};

      span {
        display: inline-block;
        margin: 0 8px;
        font-weight: bold;
        color: ${token.colorText};
        font-size: 16px;
      }
    `,
    addBtn: css`
      background: #1677ff0f;
      border: 1px solid #1677ff34;
      width: calc(100% - 24px);
      margin: 12px;
    `,
    markdown: css`
      font-size: 14px;
      line-height: 1.6;
      white-space: pre-wrap;

      h1, h2, h3, h4, h5, h6 {
        margin-top: 16px;
        margin-bottom: 8px;
        font-weight: 600;
      }

      h1 { font-size: 24px; }
      h2 { font-size: 20px; }
      h3 { font-size: 18px; }
      h4 { font-size: 16px; }
      h5 { font-size: 14px; }
      h6 { font-size: 12px; }

      p {
        margin: 8px 0;
      }

      ul, ol {
        margin: 8px 0;
        padding-left: 24px;
      }

      code {
        padding: 2px 4px;
        font-size: 90%;
        color: #c7254e;
        background-color: #f9f2f4;
        border-radius: 4px;
      }

      pre {
        padding: 12px;
        margin: 8px 0;
        overflow: auto;
        font-size: 13px;
        line-height: 1.45;
        background-color: #f6f8fa;
        border-radius: 6px;

        code {
          padding: 0;
          color: inherit;
          background-color: transparent;
          border-radius: 0;
        }
      }

      blockquote {
        margin: 8px 0;
        padding: 0 12px;
        color: #6a737d;
        border-left: 4px solid #dfe2e5;
      }

      table {
        margin: 8px 0;
        border-collapse: collapse;
        width: 100%;

        th, td {
          padding: 6px 12px;
          border: 1px solid #dfe2e5;
        }

        th {
          font-weight: 600;
          background-color: #f6f8fa;
        }
      }

      img {
        max-width: 100%;
        height: auto;
      }

      a {
        color: #1890ff;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    `,
  };
});

const promptsItems: GetProp<typeof Prompts, 'items'> = [
  {
    key: '1',
    label: renderTitle(<BookOutlined style={{ color: '#1890FF' }} />, '阅读理解'),
    disabled: true,
    description: '帮助理解和分析阅读内容',
    children: [
      {
        key: '1-1',
        description: '请帮我总结这段内容的主要观点和论述',
      },
      {
        key: '1-2',
        description: '请解释这段文字中的关键概念和术语',
      },
      {
        key: '1-3',
        description: '请分析这段内容的逻辑结构和论证方式',
      },
    ],
  },
  {
    key: '2',
    label: renderTitle(<FileTextOutlined style={{ color: '#52C41A' }} />, '内容分析'),
    disabled: true,
    description: '深入分析和对比文本内容',
    children: [
      {
        key: '2-1',
        description: '请对比分析这两段内容的异同点',
      },
      {
        key: '2-2',
        description: '请评价这个观点的优缺点和适用性',
      },
      {
        key: '2-3',
        description: '请分析这个论点的支持证据和可能的反驳',
      },
    ],
  },
  {
    key: '3',
    label: renderTitle(<QuestionCircleOutlined style={{ color: '#FF4D4F' }} />, '笔记总结'),
    disabled: true,
    description: '生成笔记和知识点总结',
    children: [
      {
        key: '3-1',
        description: '请帮我制作这章内容的思维导图',
      },
      {
        key: '3-2',
        description: '请总结这部分内容的关键知识点',
      },
      {
        key: '3-3',
        description: '请为这章内容生成复习要点和练习题',
      },
    ],
  },
  {
    key: '4',
    label: renderTitle(<RobotOutlined style={{ color: '#722ED1' }} />, '知识应用'),
    disabled: true,
    description: '知识迁移和实践应用',
    children: [
      {
        key: '4-1',
        description: '请举例说明这个概念在实际中的应用场景',
      },
      {
        key: '4-2',
        description: '请结合实例解释这个理论的现实意义',
      },
      {
        key: '4-3',
        description: '请提供一些练习方法来掌握这个知识点',
      },
    ],
  },
];

const senderPromptsItems: GetProp<typeof Prompts, 'items'> = [
  {
    key: '1',
    description: '阅读理解',
    icon: <BookOutlined style={{ color: '#1890FF' }} />,
    disabled: true,
  },
  {
    key: '2',
    description: '内容分析',
    icon: <FileTextOutlined style={{ color: '#52C41A' }} />,
    disabled: true,
  },
  {
    key: '3',
    description: '笔记总结',
    icon: <QuestionCircleOutlined style={{ color: '#FF4D4F' }} />,
    disabled: true,
  },
  {
    key: '4',
    description: '知识应用',
    icon: <RobotOutlined style={{ color: '#722ED1' }} />,
    disabled: true,
  },
];

const roles: GetProp<typeof Bubble.List, 'roles'> = {
  assistant: {
    placement: 'start',
    typing: { step: 5, interval: 20 },
    avatar: <Avatar icon={<RobotOutlined />} style={{ backgroundColor: '#52c41a' }} />,
    styles: {
      content: {
        borderRadius: 16,
      },
    },
  },
  user: {
    placement: 'end',
    variant: 'shadow',
    avatar: <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }} />,
  },
};

const renderMarkdown = (content: string, styles: any) => {
  const html = md.render(content);
  return (
    <div 
      dangerouslySetInnerHTML={{ __html: html }} 
      className={styles.markdown}
      style={{ wordBreak: 'break-word' }}
    />
  );
};

type ConversationItem = GetProp<ConversationsProps, 'items'>[number];

const AIAssistant: React.FC = () => {
  const { styles } = useStyle();
  const [open, setOpen] = useState(false);
  const [headerOpen, setHeaderOpen] = useState(false);
  const [content, setContent] = useState('');
  const [conversationsItems, setConversationsItems] = useState<ConversationItem[]>(defaultConversationsItems);
  const [activeKey, setActiveKey] = useState(defaultConversationsItems[0].key);
  const [attachedFiles, setAttachedFiles] = useState<GetProp<typeof Attachments, 'items'>>([]);
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<Array<{
    id: string;
    message: string;
    status: 'local' | 'loading' | 'success';
  }>>([]);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingConversation, setEditingConversation] = useState<ConversationItem | null>(null);
  const [newConversationName, setNewConversationName] = useState('');
  const [selectedModel, setSelectedModel] = useState('DeepSeek-V3');

  const modelOptions = [
    { label: 'DeepSeek-V3', value: 'deepseek-chat' },
    { label: 'DeepSeek-R1', value: 'deepseek-reasoner' },
  ];

  const handleSend = async (content: string) => {
    try {
      setLoading(true);
      // 添加用户消息
      const userMessage = {
        id: Date.now().toString(),
        message: content,
        status: 'local' as const,
      };
      setMessages((prev) => [...prev, userMessage]);

      // 添加一个空的助手消息，用于流式显示
      const assistantMessage = {
        id: (Date.now() + 1).toString(),
        message: '',
        status: 'loading' as const,
      };
      setMessages((prev) => [...prev, assistantMessage]);

      // 调用 DeepSeek API
      await streamChatCompletion(
        {
          messages: messages
            .concat(userMessage)
            .map((msg) => ({ role: msg.status === 'local' ? 'user' : 'assistant', content: msg.message })),
          stream: true,
          model: selectedModel,
        },
        (text) => {
          // 更新助手消息的内容
          setMessages((prev) => {
            const newMessages = [...prev];
            const lastMessage = newMessages[newMessages.length - 1];
            if (lastMessage.status === 'loading') {
              lastMessage.message += text;
            }
            return newMessages;
          });
        },
        (error) => {
          message.error('对话出错：' + error.message);
          setMessages((prev) => prev.slice(0, -1)); // 移除空的助手消息
        },
        () => {
          setMessages((prev) => {
            const newMessages = [...prev];
            const lastMessage = newMessages[newMessages.length - 1];
            if (lastMessage.status === 'loading') {
              lastMessage.status = 'success';
            }
            return newMessages;
          });
          setLoading(false);
        },
      );
    } catch (error) {
      message.error('发送消息失败');
      setLoading(false);
    }
  };

  useEffect(() => {
    if (activeKey !== undefined) {
      setMessages([]);
    }
  }, [activeKey]);

  const onSubmit = (nextContent: string) => {
    if (!nextContent) return;
    handleSend(nextContent);
    setContent('');
  };

  const onPromptsItemClick: GetProp<typeof Prompts, 'onItemClick'> = (info) => {
    if (typeof info.data.description === 'string') {
      handleSend(info.data.description);
    }
  };

  const handleFileChange: GetProp<typeof Attachments, 'onChange'> = (info) =>
    setAttachedFiles(info.fileList);

  const handleCopy = (content: string) => {
    navigator.clipboard.writeText(content).then(() => {
      message.success('已复制到剪贴板');
    });
  };

  const handleReuse = (content: string) => {
    setContent(content);
  };

  // 处理会话重命名
  const handleRename = (conversation: ConversationItem) => {
    setEditingConversation(conversation);
    setNewConversationName(String(conversation.label));
    setEditModalVisible(true);
  };

  // 处理删除会话
  const handleDelete = (key: string) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个会话吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        setConversationsItems(items => items.filter(item => item.key !== key));
        if (activeKey === key) {
          // 如果删除的是当前活动的会话，切换到第一个会话
          const firstItem = conversationsItems.find(item => item.key !== key);
          if (firstItem) {
            setActiveKey(firstItem.key);
          }
        }
        message.success('会话已删除');
      },
    });
  };

  // 处理重命名确认
  const handleRenameConfirm = () => {
    if (editingConversation && newConversationName.trim()) {
      setConversationsItems(items =>
        items.map(item =>
          item.key === editingConversation.key
            ? { ...item, label: newConversationName.trim() }
            : item
        )
      );
      setEditModalVisible(false);
      setEditingConversation(null);
      setNewConversationName('');
      message.success('会话已重命名');
    }
  };

  // 配置会话菜单
  const menuConfig: ConversationsProps['menu'] = (conversation) => ({
    items: [
      {
        label: '重命名',
        key: 'rename',
        icon: <EditOutlined />,
        onClick: () => handleRename(conversation),
      },
      {
        label: '删除',
        key: 'delete',
        icon: <DeleteOutlined />,
        danger: true,
        onClick: () => handleDelete(conversation.key),
      },
    ],
  });

  const placeholderNode = (
    <Space direction="vertical" size={16} className={styles.placeholder}>
      <Welcome
        variant="borderless"
        icon={<RobotOutlined style={{ fontSize: 48, color: '#52c41a' }} />}
        title="律π 助手"
        description={<span style={{ color: 'red', fontSize: '16px' }}>Prompt功能正在开发中... 目前仅支持文本对话</span>}
      />
      <Prompts
        title="您需要什么帮助？"
        items={promptsItems}
        styles={{
          list: {
            width: '100%',
          },
          item: {
            flex: 1,
          },
        }}
        onItemClick={onPromptsItemClick}
      />
    </Space>
  );

  const items: GetProp<typeof Bubble.List, 'items'> = messages.map(({ id, message, status }) => ({
    key: id,
    loading: status === 'loading',
    role: status === 'local' ? 'user' : 'assistant',
    content: message,
    messageRender: status === 'local' ? undefined : (content) => renderMarkdown(content, styles),
    avatar: status === 'local' ? (
      <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }} />
    ) : (
      <Avatar icon={<RobotOutlined />} style={{ backgroundColor: '#52c41a' }} />
    ),
    footer: status === 'local' ? (
      <Button 
        type="text" 
        size="small" 
        icon={<PlusOutlined />} 
        onClick={() => handleReuse(message)}
      >
        重新使用
      </Button>
    ) : (
      <Button 
        type="text" 
        size="small" 
        icon={<CopyOutlined />} 
        onClick={() => handleCopy(message)}
      >
        复制
      </Button>
    ),
  }));

  const attachmentsNode = (
    <Badge dot={attachedFiles.length > 0 && !headerOpen}>
      <Button type="text" icon={<PaperClipOutlined />} onClick={() => setHeaderOpen(!headerOpen)} />
    </Badge>
  );

  const senderHeader = (
    <Sender.Header
      title="附件"
      open={headerOpen}
      onOpenChange={setHeaderOpen}
      styles={{
        content: {
          padding: 0,
        },
      }}
    >
      <Attachments
        beforeUpload={() => false}
        items={attachedFiles}
        onChange={handleFileChange}
        placeholder={(type) =>
          type === 'drop'
            ? { title: '拖拽文件到这里' }
            : {
                icon: <CloudUploadOutlined />,
                title: '上传文件',
                description: '点击或拖拽文件到此区域上传',
              }
        }
      />
    </Sender.Header>
  );

  const logoNode = (
    <div className={styles.logo}>
      <RobotOutlined style={{ fontSize: 24, marginRight: 8 }} />
      <span>律π 助手</span>
    </div>
  );

  return (
    <>
      <FloatButton
        icon={<RobotOutlined />}
        type="primary"
        style={{ right: 24, bottom: 100 }}
        onClick={() => setOpen(true)}
      />
      <Modal
        title={null}
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={1200}
        bodyStyle={{ padding: 0, height: 722 }}
        destroyOnClose
        centered
        styles={{
          mask: {
            backgroundColor: 'rgba(0, 0, 0, 0.45)',
            backdropFilter: 'blur(4px)',
          },
          content: {
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.12)',
          },
        }}
      >
        <div className={styles.layout}>
          <div className={styles.menu}>
            {logoNode}
            <Button
              type="text"
              icon={<PlusOutlined />}
              className={styles.addBtn}
              onClick={() => {
                const newKey = String(Date.now());
                setConversationsItems(prev => [
                  ...prev,
                  { key: newKey, label: `新的对话 ${prev.length + 1}` },
                ]);
                setActiveKey(newKey);
              }}
            >
              新的对话
            </Button>
            <div className={styles.conversations}>
              <Conversations
                activeKey={activeKey}
                items={conversationsItems}
                menu={menuConfig}
                onActiveChange={(key: string) => setActiveKey(key)}
              />
            </div>
            <div style={{fontSize: '16px',fontWeight: 'bold', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 12px' }}>
              <span>大模型：</span>
              <Select
                defaultValue={selectedModel}
                onChange={setSelectedModel}
                style={{ width: 180, marginLeft: 'auto' }}
              >
                {modelOptions.map(option => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </div>
          </div>
          <div className={styles.chat}>
            <Bubble.List
              items={items.length > 0 ? items : [{ content: placeholderNode, variant: 'borderless' }]}
              roles={roles}
              className={styles.messages}
            />
            <Prompts items={senderPromptsItems} onItemClick={onPromptsItemClick} />
            <Sender
              value={content}
              header={senderHeader}
              onSubmit={onSubmit}
              onChange={setContent}
              prefix={attachmentsNode}
              loading={loading}
              className={styles.sender}
            />
          </div>
        </div>
      </Modal>
      <Modal
        title="重命名会话"
        open={editModalVisible}
        onOk={handleRenameConfirm}
        onCancel={() => {
          setEditModalVisible(false);
          setEditingConversation(null);
          setNewConversationName('');
        }}
        okText="确定"
        cancelText="取消"
      >
        <Input
          value={newConversationName}
          onChange={e => setNewConversationName(e.target.value)}
          placeholder="请输入新的会话名称"
          autoFocus
        />
      </Modal>
    </>
  );
};

export default AIAssistant; 