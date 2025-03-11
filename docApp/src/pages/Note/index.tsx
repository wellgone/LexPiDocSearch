import React, { useRef, useState, useEffect } from 'react';
import { ActionType, ProCard, ProColumns, ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, message, Splitter, Card, Input, Select, Dropdown, Space } from 'antd';
import { SortableTree } from './components/SortableTree';
import type { TreeItems } from './types';
import styles from './index.less';
import { LpNote, LpSearchReport, getUserReports, getReportNoteTree, batchUpdateNoteOrder, updateNote, batchUpdateNoteHierarchy, deleteNote, batchDeleteNotes, createReport, listReports, deleteReport, updateReport } from './service';
import { DeleteOutlined, LeftOutlined, DownOutlined, CopyOutlined, PlusOutlined, MoreOutlined, EyeOutlined, EditOutlined, FileSearchOutlined } from '@ant-design/icons';

interface NoteItem {
  id: string;
  children: NoteItem[];
  content?: string;
  collapsed?: boolean;
  pageSize?: number;
  sourceName?: string;
  sourcePress?: string;
  sourceAuthor?: string;
  sourcePublicationDate?: string;
  parentId?: string | null;
  orderNum?: number;
}

interface NoteGroup {
  sourceName: string;
  notes: NoteItem[];
  collapsed?: boolean;
  sourcePageSize?: string;
  sourceType?: string;
  sourcePress?: string;
  sourceAuthor?: string;
  sourcePublicationDate?: string;
}

const NotePage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [noteGroups, setNoteGroups] = useState<NoteGroup[]>([]);
  const [currentReport, setCurrentReport] = useState<LpSearchReport>();
  const [editingNote, setEditingNote] = useState<NoteItem | null>(null);
  const [form] = Form.useForm();
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [createForm] = Form.useForm();
  const [createLoading, setCreateLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [userId, setUserId] = useState<string | null>(null);
  const pageSize = 10;
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editForm] = Form.useForm();
  const [editLoading, setEditLoading] = useState(false);

  // 初始化时获取用户ID
  useEffect(() => {
    const storedUserId = localStorage.getItem('userId');
    if (!storedUserId) {
      message.error('获取用户信息失败，请重新登录');
      window.location.href = '/user/login';
      return;
    }
    setUserId(storedUserId);
  }, []);

  // 初始化时加载第一个报告
  useEffect(() => {
    const loadFirstReport = async () => {
      try {
        if (!userId) {
          return;
        }
        const response = await getUserReports(userId);
        if (response.data && response.data.length > 0) {
          const firstReport = response.data[0];
          setCurrentReport(firstReport);
          loadReportNotes(firstReport.id);
        }
      } catch (error) {
        message.error('加载检索报告失败');
      }
    };
    loadFirstReport();
  }, [userId]);

  // 按书籍分组处理笔记
  const groupNotesByDoc = (notes: LpNote[]) => {
    const groups: { [key: string]: { notes: NoteItem[]; metadata?: any } } = {};
    
    const processNote = (note: LpNote) => {
      const sourceName = note.sourceName || '未分类';
      if (!groups[sourceName]) {
        groups[sourceName] = {
          notes: [],
          metadata: {
            sourcePress: note.sourcePress,
            sourceAuthor: note.sourceAuthor,
            sourcePublicationDate: note.sourcePublicationDate,
          }
        };
      }

      const processChildren = (children: LpNote[]): NoteItem[] => {
        return children.map(child => ({
          id: String(child.id),
          content: child.content,
          children: child.children ? processChildren(child.children) : [],
          pageSize: child.sourcePageSize,
          sourceName: child.sourceName,
          sourcePress: child.sourcePress,
          sourceAuthor: child.sourceAuthor,
          sourcePublicationDate: child.sourcePublicationDate,
          parentId: child.parentId,
        }));
      };

      const noteItem: NoteItem = {
        id: String(note.id),
        content: note.content,
        children: note.children ? processChildren(note.children) : [],
        pageSize: note.sourcePageSize,
        sourceName: note.sourceName,
        sourcePress: note.sourcePress,
        sourceAuthor: note.sourceAuthor,
        sourcePublicationDate: note.sourcePublicationDate,
        parentId: note.parentId,
      };

      groups[sourceName].notes.push(noteItem);
    };

    // 只处理顶层笔记
    notes.filter(note => !note.parentId).forEach(processNote);
    
    return Object.entries(groups).map(([sourceName, {notes, metadata}]) => ({
      sourceName,
      notes,
      collapsed: false,
      ...metadata
    }));
  };

  // 处理笔记编辑
  const handleEditNote = async (id: string, content: string) => {
    try {
      if (!userId) {
        message.error('获取用户信息失败，请重新登录');
        window.location.href = '/user/login';
        return;
      }
      // 在笔记组中找到对应的笔记
      let targetNote: NoteItem | null = null;
      let targetGroup: NoteGroup | null = null;

      for (const group of noteGroups) {
        const findNoteInGroup = (notes: NoteItem[]): NoteItem | null => {
          for (const note of notes) {
            if (note.id === id) {
              return note;
            }
            if (note.children) {
              const found = findNoteInGroup(note.children);
              if (found) return found;
            }
          }
          return null;
        };

        const found = findNoteInGroup(group.notes);
        if (found) {
          targetNote = found;
          targetGroup = group;
          break;
        }
      }

      if (!targetNote) {
        throw new Error('笔记不存在');
      }

      // 准备更新参数，确保与后端API格式一致
      const updateParams = {
        id: Number(id),
        content,
        sourceName: targetGroup?.sourceName,
        sourcePress: targetGroup?.sourcePress,
        sourceAuthor: targetGroup?.sourceAuthor,
        sourcePublicationDate: targetGroup?.sourcePublicationDate,
        sourcePageSize: targetNote.pageSize,
        modifiedTime: new Date().toISOString(),
        userId: Number(userId),
        parentId: targetNote.parentId ? Number(targetNote.parentId) : undefined,
        orderNum: targetNote.orderNum
      };

      // 调用更新接口
      const response = await updateNote(updateParams);
      
      if (response.code === 0) {
        // 更新本地状态
        const updateNoteInGroups = (groups: NoteGroup[]): NoteGroup[] => {
          return groups.map(group => ({
            ...group,
            notes: updateNoteContent(group.notes, id, content)
          }));
        };

        const updateNoteContent = (items: NoteItem[], noteId: string, newContent: string): NoteItem[] => {
          return items.map(item => {
            if (item.id === noteId) {
              return { ...item, content: newContent };
            }
            if (item.children?.length) {
              return { ...item, children: updateNoteContent(item.children, noteId, newContent) };
            }
            return item;
          });
        };

        setNoteGroups(updateNoteInGroups(noteGroups));
        message.success('笔记已更新');
      } else {
        message.error(response.message || '更新失败');
      }
    } catch (error) {
      message.error('更新笔记失败');
    }
  };

  // TreeItem的编辑回调处理
  const handleTreeItemEdit = (id: string, content: string) => {
    // 直接调用handleEditNote，不再打开模态框
    handleEditNote(id, content);
  };

  // 打开编辑模态框
  // const handleEditClick = (note: NoteItem) => {
  //   setEditingNote(note);
  //   form.setFieldsValue({
  //     content: note.content
  //   });
  // };

  // 处理编辑提交
  const handleEditSubmit = async () => {
    try {
      setEditLoading(true);
      const values = await editForm.validateFields();
      
      if (!userId) {
        message.error('获取用户信息失败，请重新登录');
        window.location.href = '/user/login';
        return;
      }
      
      const response = await updateReport({
        id: values.id,
        title: values.title,
        type: values.type,
        userId: userId,
        modifiedTime: new Date().toISOString()
      });
      
      if (response.code === 0) {
        message.success('检索报告更新成功');
        setEditModalVisible(false);
        editForm.resetFields();
        // 刷新列表
        actionRef.current?.reload();
        // 如果编辑的是当前报告，更新当前报告信息
        if (currentReport?.id === values.id) {
          setCurrentReport({
            ...currentReport,
            title: values.title,
            type: values.type,
            modifiedTime: new Date().toISOString()
          });
        }
      } else {
        message.error(response.message || '更新失败');
      }
    } catch (error: any) {
      console.error('更新检索报告失败:', error);
      message.error(error.message || '更新检索报告失败，请重试');
    } finally {
      setEditLoading(false);
    }
  };

  // 处理笔记删除
  const handleRemoveNote = async (id: string) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个笔记吗？其子笔记将一并删除，且删除后将无法恢复。',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await deleteNote(Number(id));
          if (response.code === 0) {
            // 更新前端状态
            const removeFromNotes = (items: NoteItem[]): NoteItem[] => {
              return items.filter(item => {
                if (item.id === id) {
                  return false;
                }
                if (item.children.length) {
                  item.children = removeFromNotes(item.children);
                }
                return true;
              });
            };

            // 更新笔记组状态
            setNoteGroups(prevGroups => 
              prevGroups.map(group => ({
                ...group,
                notes: removeFromNotes(group.notes)
              }))
            );

            message.success('笔记已删除');
          } else {
            message.error(response.message || '删除失败');
          }
        } catch (error) {
          console.error('删除笔记失败:', error);
          message.error('删除笔记失败');
        }
      },
    });
  };

  // 加载检索报告的笔记
  const loadReportNotes = async (reportId: number) => {
    try {
      const response = await getReportNoteTree(reportId);
      if (response.data) {
        // 递归转换笔记数据为 NoteItem 格式
        const convertToNoteItems = (notes: LpNote[]): NoteItem[] => {
          return notes.map(note => ({
            id: String(note.id),
            content: note.content,
            children: note.children ? convertToNoteItems(note.children) : [],
            pageSize: note.sourcePageSize,
            sourceName: note.sourceName,
          }));
        };

        const convertedNotes = convertToNoteItems(response.data);
        const groups = groupNotesByDoc(response.data);
        
        setNoteGroups(groups);
      }
    } catch (error) {
      message.error('加载笔记失败');
    }
  };

  // 处理分组折叠状态
  const handleGroupCollapse = (index: number) => {
    setNoteGroups(prev => prev.map((group, i) => 
      i === index ? { ...group, collapsed: !group.collapsed } : group
    ));
  };

  // 处理删除分组
  const handleDeleteGroup = (index: number, sourceName: string) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除 "${sourceName}" 分组下的所有笔记吗？删除后将无法恢复。`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 获取该分组下所有笔记的ID
          const groupNotes = noteGroups[index].notes;
          const collectNoteIds = (notes: NoteItem[]): number[] => {
            return notes.reduce((ids: number[], note) => {
              ids.push(Number(note.id));
              if (note.children?.length) {
                ids.push(...collectNoteIds(note.children));
              }
              return ids;
            }, []);
          };
          
          const noteIds = collectNoteIds(groupNotes);
          
          // 调用批量删除API
          const response = await batchDeleteNotes(noteIds);
          
          if (response.code === 0) {
            // 更新前端状态
            setNoteGroups(prev => prev.filter((_, i) => i !== index));
            message.success('笔记分组已删除');
          } else {
            message.error(response.message || '删除笔记分组失败');
          }
        } catch (error) {
          console.error('删除笔记分组失败:', error);
          message.error('删除笔记分组失败');
        }
      },
    });
  };

  // 处理笔记层级和排序变更
  const handleNoteMove = async (items: NoteItem[], isCollapsedChange = false, groupIndex: number) => {
    // 如果只是折叠状态变更，直接更新前端状态，不触发后端更新
    if (isCollapsedChange) {
      setNoteGroups(prevGroups => {
        const newGroups = [...prevGroups];
        newGroups[groupIndex].notes = items;
        return newGroups;
      });
      return;
    }

    try {
      // 收集所有需要更新的笔记
      const updates: { id: number; parentId: number; orderNum: number }[] = [];
      
      const processItems = (items: NoteItem[], parentId: number = 0, startOrder: number = 0) => {
        items.forEach((item, index) => {
          updates.push({
            id: Number(item.id),
            parentId,
            orderNum: startOrder + index
          });
          
          if (item.children?.length) {
            processItems(item.children, Number(item.id), 0);
          }
        });
      };
      
      processItems(items);
      
      // 批量更新排序和层级
      await batchUpdateNoteHierarchy({ notes: updates });
      
      // 更新前端状态
      setNoteGroups(prevGroups => {
        const newGroups = [...prevGroups];
        newGroups[groupIndex].notes = items;
        return newGroups;
      });
      
      message.success('笔记结构已更新');
    } catch (error) {
      console.error('更新笔记结构失败:', error);
      message.error('更新笔记结构失败');
    }
  };

  // 处理复制整组笔记
  const handleCopyGroup = (group: NoteGroup) => {
    const formatNotes = (notes: NoteItem[]): string => {
      return notes.map((note, index) => {
        const pageInfo = note.pageSize ? `——P${note.pageSize}` : '';
        const noteText = `${index + 1}. ${note.content}${pageInfo}`;
        
        if (note.children && note.children.length > 0) {
          return `${noteText}\n${formatNotes(note.children)}`;
        }
        return noteText;
      }).join('\n');
    };

    const headerInfo = `《${group.sourceName}》 ${group.sourceAuthor ? ` ${group.sourceAuthor}` : ''}${group.sourcePress ? ` ${group.sourcePress}` : ''}${group.sourcePublicationDate ? ` ${group.sourcePublicationDate}` : ''}\n\n`;
    const notesText = formatNotes(group.notes);
    const textToCopy = headerInfo + notesText;

    navigator.clipboard.writeText(textToCopy).then(() => {
      message.success('已复制整组笔记到剪贴板');
    }).catch(() => {
      message.error('复制失败');
    });
  };

  // 处理创建检索报告
  const handleCreateReport = async () => {
    try {
      setCreateLoading(true);
      const values = await createForm.validateFields();
      
      if (!userId) {
        message.error('获取用户信息失败，请重新登录');
        window.location.href = '/user/login';
        return;
      }
      
      const response = await createReport({
        title: values.title,
        type: values.type,
        userId: userId,
        searchSubject: 0,
        createTime: new Date().toISOString(),
        modifiedTime: new Date().toISOString()
      });
      
      if (response.code === 0) {
        message.success('检索报告创建成功');
        setCreateModalVisible(false);
        createForm.resetFields();
        // 刷新列表
        actionRef.current?.reload();
        // 如果是第一个报告，直接加载它
        const reportsResponse = await getUserReports(userId);
        if (reportsResponse.data && (!currentReport || reportsResponse.data.length === 1)) {
          const firstReport = reportsResponse.data[0];
          setCurrentReport(firstReport);
          loadReportNotes(firstReport.id);
        }
      } else {
        message.error(response.message || '创建失败');
      }
    } catch (error: any) {
      console.error('创建检索报告失败:', error);
      message.error(error.message || '创建检索报告失败，请重试');
    } finally {
      setCreateLoading(false);
    }
  };

  // 处理删除报告
  const handleDeleteReport = (record: LpSearchReport) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除报告"${record.title}"吗？删除后将无法恢复。`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await deleteReport(record.id);
          if (response.code === 0) {
            message.success('报告已删除');
            // 如果删除的是当前选中的报告，清空当前报告
            if (currentReport?.id === record.id) {
              setCurrentReport(undefined);
              setNoteGroups([]);
            }
            // 刷新列表
            actionRef.current?.reload();
          } else {
            message.error(response.message || '删除失败');
          }
        } catch (error) {
          console.error('删除报告失败:', error);
          message.error('删除报告失败');
        }
      },
    });
  };

  // 处理设为检索菜单子选项
  const handleSetAsSearchSubject = async (record: LpSearchReport) => {
    try {
      if (!userId) {
        message.error('获取用户信息失败，请重新登录');
        window.location.href = '/user/login';
        return;
      }

      const newSearchSubject = record.searchSubject === 1 ? 0 : 1;
      const response = await updateReport({
        id: record.id,
        searchSubject: newSearchSubject,
        userId: userId,
        modifiedTime: new Date().toISOString()
      });

      if (response.code === 0) {
        message.success(newSearchSubject === 1 ? '设置检索菜单成功' : '取消检索菜单成功');
        // 更新当前报告状态
        if (currentReport?.id === record.id) {
          setCurrentReport({
            ...currentReport,
            searchSubject: newSearchSubject,
            modifiedTime: new Date().toISOString()
          });
        }
        // 刷新列表
        actionRef.current?.reload();
      } else {
        message.error(response.message || '设置失败');
      }
    } catch (error) {
      console.error('设置检索菜单失败:', error);
      message.error('设置失败');
    }
  };

  // 处理编辑报告
  const handleEditReport = (record: LpSearchReport) => {
    setEditModalVisible(true);
    editForm.setFieldsValue({
      title: record.title,
      type: record.type,
      id: record.id
    });
  };

  // 检索报告列表列定义
  const columns: ProColumns<LpSearchReport>[] = [
    {
      title: '报告名称',
      dataIndex: 'title',
      search: true
    },
    {
      title: '关联菜单',
      dataIndex: 'searchSubject', 
      search: false,
      render: (text, record) => (
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <span>
            {record.searchSubject === 1 ? <FileSearchOutlined style={{ color: '#1890ff', fontSize: '16px' }} /> : ''}
          </span>
        </div>
      ),
    },
    {
      title: '修改时间',
      dataIndex: 'modifiedTime',
      valueType: 'dateTime',
      hideInTable: true,
      search: false,
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      render: (_, record) => [
        <Dropdown
          key="more"
          menu={{
            items: [
              {
                key: 'read',
                icon: <FileSearchOutlined />,
                label: record.searchSubject === 1 ? '取消关联菜单' : '设置关联菜单',
                onClick: () => handleSetAsSearchSubject(record),
              },
              {
                key: 'edit',
                icon: <EditOutlined />,
                label: '编辑报告',
                onClick: () => handleEditReport(record),
              },
              {
                key: 'delete',
                icon: <DeleteOutlined />,
                label: '删除报告',
                danger: true,
                onClick: () => handleDeleteReport(record),
              },
            ],
          }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Button
            type="text"
            icon={<MoreOutlined />}
            onClick={(e) => e.stopPropagation()}
          />
        </Dropdown>,
      ],
    },
  ];

  return (
    <>
      <Splitter>
        <Splitter.Panel defaultSize="20%" min="20%" max="40%">
          <Card style={{padding:0, height:'94vh',overflow: 'auto'}}>
            <div style={{ 
              padding: '12px 6px', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'space-between',
              borderBottom: '1px solid #f0f0f0'
            }}>
              <Input.Search
                placeholder="搜索检索报告"
                onSearch={async (value) => {
                  setSearchKeyword(value);
                  setCurrentPage(1);
                  actionRef.current?.reload();
                }}
                style={{ marginRight: '12px' }}
                allowClear
              />
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setCreateModalVisible(true)}
              >
                新建报告
              </Button>
            </div>
            <ProTable<LpSearchReport>
              actionRef={actionRef}
              ghost
              rowKey="id"
              search={false}
              options={false}
              toolbar={false}
              request={async () => {
                try {
                  const response = await listReports(currentPage, pageSize, searchKeyword);
                  if (response.code === 0) {
                    return {
                      data: response.data.records,
                      success: true,
                      total: response.data.total,
                    };
                  }
                  message.error(response.message || '加载检索报告失败');
                  return {
                    data: [],
                    success: false,
                    total: 0,
                  };
                } catch (error) {
                  message.error('加载检索报告失败');
                  return {
                    data: [],
                    success: false,
                    total: 0,
                  };
                }
              }}
              columns={columns}
              pagination={{
                pageSize,
                current: currentPage,
                onChange: (page) => setCurrentPage(page),
              }}
              size="small"
              onRow={(record) => {
                return {
                  onClick: () => {
                    setCurrentReport(record);
                    loadReportNotes(record.id);
                  },
                  style: {
                    backgroundColor: 
                      currentReport?.id === record.id ? '#e6f7ff' : undefined,
                  },
                };
              }}
            />
          </Card>
        </Splitter.Panel>
        <Splitter.Panel>
          <ProCard
            title={currentReport?.title || '检索报告'}
            extra={currentReport ? `创建时间：${currentReport.createTime}` : undefined}
            style={{ height: '94vh'}}
            bodyStyle={{ overflow: 'auto' }}
            headerBordered
          >
            <div className={styles.noteContainer}>
              <div className={styles.groupList}>
                {noteGroups.map((group, groupIndex) => (
                  <div key={group.sourceName} className={styles.groupItem}>
                    <div className={styles.groupContent}>
                      <div className={styles.groupHeader}>
                        <span className={styles.groupIndex}>{groupIndex + 1}.</span>
                        <span className={styles.groupTitle}>
                          {group.sourceName}
                          {(group.sourcePress || group.sourceAuthor || group.sourcePublicationDate) && (
                            <span className={styles.sourceInfo}>
                              {group.sourcePress && <span className={styles.sourcePress}>{group.sourcePress}</span>}
                              {group.sourceAuthor && <span className={styles.sourceAuthor}>{group.sourceAuthor}</span>}
                              {group.sourcePublicationDate && (
                                <span className={styles.sourceDate}>
                                  {/* {new Date(group.sourcePublicationDate).getFullYear()} */}
                                  {group.sourcePublicationDate}
                                </span>
                              )}
                            </span>
                          )}
                        </span>
                        <div className={styles.groupActions}>
                          <Button
                            type="text"
                            className={styles.copyButton}
                            icon={<CopyOutlined />}
                            onClick={() => handleCopyGroup(group)}
                            title="复制整组笔记"
                          />
                          <Button
                            type="text"
                            danger
                            className={styles.deleteButton}
                            icon={<DeleteOutlined />}
                            onClick={() => handleDeleteGroup(groupIndex, group.sourceName)}
                          />
                          <Button
                            type="text"
                            className={styles.deleteButton}
                            icon={group.collapsed ? <LeftOutlined /> : <DownOutlined />}
                            onClick={() => handleGroupCollapse(groupIndex)}
                          />
                        </div>
                      </div>
                      {!group.collapsed && (
                        <div className={styles.groupNotes}>
                          <SortableTree
                            collapsible
                            indicator
                            removable
                            items={group.notes as TreeItems}
                            onItemsChange={(items, isCollapsedChange) => {
                              handleNoteMove(items as NoteItem[], isCollapsedChange, groupIndex);
                            }}
                            onEditNote={handleTreeItemEdit}
                            onRemoveNote={handleRemoveNote}
                            groupIndex={groupIndex}
                          />
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </ProCard>
        </Splitter.Panel>
      </Splitter>

      <Modal
        title="新建检索报告"
        open={createModalVisible}
        onOk={handleCreateReport}
        onCancel={() => {
          setCreateModalVisible(false);
          createForm.resetFields();
        }}
        confirmLoading={createLoading}
      >
        <Form 
          form={createForm}
          layout="vertical"
        >
          <Form.Item
            name="title"
            label="报告名称"
            rules={[
              { required: true, message: '请输入报告名称' },
              { max: 50, message: '报告名称不能超过50个字符' }
            ]}
          >
            <Input placeholder="请输入报告名称" maxLength={50} showCount />
          </Form.Item>
          <Form.Item
            name="type"
            label="报告类型"
            initialValue="MANUAL"
            rules={[
              { required: true, message: '请选择报告类型' }
            ]}
          >
            <Select>
              <Select.Option value="MANUAL">手动创建</Select.Option>
              <Select.Option value="AUTO">自动生成</Select.Option>
              <Select.Option value="IMPORT">导入</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑报告模态框 */}
      <Modal
        title="编辑检索报告"
        open={editModalVisible}
        onOk={handleEditSubmit}
        onCancel={() => {
          setEditModalVisible(false);
          editForm.resetFields();
        }}
        confirmLoading={editLoading}
      >
        <Form 
          form={editForm}
          layout="vertical"
        >
          <Form.Item
            name="id"
            hidden
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="title"
            label="报告名称"
            rules={[
              { required: true, message: '请输入报告名称' },
              { max: 50, message: '报告名称不能超过50个字符' }
            ]}
          >
            <Input placeholder="请输入报告名称" maxLength={50} showCount />
          </Form.Item>
          <Form.Item
            name="type"
            label="报告类型"
            rules={[
              { required: true, message: '请选择报告类型' }
            ]}
          >
            <Select>
              <Select.Option value="MANUAL">手动创建</Select.Option>
              <Select.Option value="AUTO">自动生成</Select.Option>
              <Select.Option value="IMPORT">导入</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default NotePage; 