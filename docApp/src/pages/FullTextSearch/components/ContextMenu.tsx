import React, { useEffect, useState } from 'react';
import { Menu, Tooltip } from 'antd';
import type { MenuProps } from 'antd';
import { RobotOutlined, SnippetsOutlined } from '@ant-design/icons';
import { type Report } from '@/services/report';
import { getSearchSubjectReports } from '@/pages/Note/service';

interface ContextMenuProps {
  visible: boolean;
  x: number;
  y: number;
  userId: string;
  selectedText: string;
  docInfo: {
    title: string;
    publisher: string;
    publicationYear: string;
    author: string;
    pageNum: number;
  };
  onClose: () => void;
  onAddToNote: (text: string, docInfo: any, reportId: number) => void;
}

const ContextMenu: React.FC<ContextMenuProps> = ({
  visible,
  x,
  y,
  selectedText,
  docInfo,
  onClose,
  onAddToNote,
}) => {
  const [reports, setReports] = useState<Report[]>([]);

  useEffect(() => {
    const fetchReports = async () => {
      try {
        const response = await getSearchSubjectReports();
        if (response.code === 0 && response.data) {
          setReports(response.data);
        }
      } catch (error) {
        console.error('获取报告列表失败:', error);
      }
    };
    
    if (visible) {
      fetchReports();
    }
  }, [visible]);

  const items: MenuProps['items'] = [
    {
      key: 'addToNote',
      label: '添加到报告',
      icon: <SnippetsOutlined />,
      children: reports.map(report => ({
        key: report.id.toString(),
        label: report.title,
        onClick: () => {
          const cleanedText = selectedText.replace(/\n/g, '');
          onAddToNote(cleanedText, docInfo, report.id);
          onClose();
        },
      })),
    },
    {
      key: 'askAI',
      icon: <RobotOutlined />,
      label: 'AI问答',
      children: [
        { key: 'summary', disabled: true, label: <Tooltip title="开发中...">总结</Tooltip> },
        { key: 'analysis', disabled: true, label: <Tooltip title="开发中...">分析</Tooltip> },
        { key: 'rewrite', disabled: true, label: <Tooltip title="开发中...">改写</Tooltip> },
        { key: 'more', disabled: true, label: <Tooltip title="开发中...">更多功能...</Tooltip> },
      ],
    },
  ];

  return visible ? (
    <div
      style={{
        position: 'fixed',
        top: y,
        left: x,
        zIndex: 1000,
      }}
    >
      <Menu style={{ minWidth: 0, flex: "auto" }} mode="vertical" items={items} />
    </div>
  ) : null;
};

export default ContextMenu; 