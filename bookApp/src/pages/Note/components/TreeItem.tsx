import React, {forwardRef, HTMLAttributes, useState, useEffect, useRef} from 'react';
import classNames from 'classnames';
import styles from './TreeItem.css';
import { DownOutlined, EditOutlined, SaveOutlined, CloseOutlined, DeleteOutlined, LeftOutlined, ExpandOutlined, CompressOutlined, CopyOutlined } from '@ant-design/icons';
import { Input, message } from 'antd';
import { updateNote } from '@/services/note';
import { Action, Handle } from './Item';

export interface Props extends Omit<HTMLAttributes<HTMLLIElement>, 'id'> {
  id: string | number;
  childCount?: number;
  clone?: boolean;
  collapsed?: boolean;
  depth: number;
  disableInteraction?: boolean;
  disableSelection?: boolean;
  ghost?: boolean;
  handleProps?: any;
  indicator?: boolean;
  indentationWidth: number;
  value: string;
  pageSize?: number;
  sourceInfo?: {
    sourceName?: string;
    sourcePress?: string;
    sourceAuthor?: string;
    sourcePublicationDate?: string;
  };
  onCollapse?(): void;
  onRemove?(): void;
  onEdit?(id: string, value: string): void;
  wrapperRef?(node: HTMLLIElement): void;
  groupIndex?: number;
  itemIndex?: number;
  parentNumber?: string;
}

export const TreeItem = forwardRef<HTMLDivElement, Props>(
  (
    {
      id,
      childCount,
      clone,
      depth,
      disableSelection,
      disableInteraction,
      ghost,
      handleProps,
      indentationWidth,
      indicator,
      collapsed,
      onCollapse,
      onRemove,
      onEdit,
      style,
      value,
      pageSize,
      sourceInfo,
      wrapperRef,
      groupIndex,
      itemIndex,
      parentNumber,
      ...props
    },
    ref
  ) => {
    const [isEditing, setIsEditing] = useState(false);
    const [editValue, setEditValue] = useState(value);
    const [isExpanded, setIsExpanded] = useState(false);
    const [maxWidth, setMaxWidth] = useState(600);
    const contentRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
      setEditValue(value);
    }, [value]);

    useEffect(() => {
      const calculateMaxWidth = () => {
        if (contentRef.current) {
          const parentWidth = contentRef.current.offsetWidth;
          // 预留空间给操作按钮和其他元素
          const reservedSpace = 150; // 根据实际按钮和间距调整
          const newMaxWidth = Math.max(parentWidth - reservedSpace, 200); // 设置最小宽度为200px
          setMaxWidth(newMaxWidth);
        }
      };

      calculateMaxWidth();
      window.addEventListener('resize', calculateMaxWidth);

      return () => {
        window.removeEventListener('resize', calculateMaxWidth);
      };
    }, []);

    const handleEditClick = () => {
      setIsEditing(true);
    };

    const handleSave = async () => {
      console.log('Saving note with id:', id, typeof id);
      console.log('Full props:', {
        id,
        value,
        pageSize,
        sourceInfo
      });

      try {
        // 准备更新参数
        const updateParams = {
          id: Number(id),
          content: editValue,
          sourceName: sourceInfo?.sourceName,
          sourcePress: sourceInfo?.sourcePress,
          sourceAuthor: sourceInfo?.sourceAuthor,
          sourcePublicationDate: sourceInfo?.sourcePublicationDate,
          sourcePageSize: pageSize,
          modifiedTime: new Date().toISOString(),
          userId: localStorage.getItem('userId') || ''
        };

        console.log('Update params:', updateParams);

        // 调用更新接口
        const response = await updateNote(updateParams);
        
        if (response.code === 0) {
          // 更新成功后，调用父组件的onEdit方法更新UI
          if (onEdit) {
            onEdit(String(id), editValue);
          }
          setIsEditing(false);
          message.success('笔记已更新');
        } else {
          message.error(response.message || '更新失败');
        }
      } catch (error) {
        console.error('更新笔记失败:', error);
        message.error('更新笔记失败');
      }
    };

    const handleCancel = () => {
      setEditValue(value);
      setIsEditing(false);
    };

    const handleExpand = () => {
      setIsExpanded(!isExpanded);
    };

    const handleCopy = () => {
      const sourceInfoText = sourceInfo ? 
        `——${sourceInfo.sourceName || ''}${sourceInfo.sourceAuthor ? ` ${sourceInfo.sourceAuthor}` : ''}${sourceInfo.sourcePress ? ` ${sourceInfo.sourcePress}` : ''}${sourceInfo.sourcePublicationDate ? ` ${sourceInfo.sourcePublicationDate}` : ''}${pageSize ? ` P${pageSize}` : ''}` : '';
      
      console.log('Copying with info:', {
        content: value,
        sourceInfo,
        pageSize
      });
      
      const textToCopy = `${value}${sourceInfoText}`;
      navigator.clipboard.writeText(textToCopy).then(() => {
        message.success('已复制到剪贴板');
      }).catch(() => {
        message.error('复制失败');
      });
    };

    const collapseIcon = collapsed ? <LeftOutlined style={{margin: '5px',color:'#3a3939d7'}} /> : <DownOutlined style={{margin: '5px',color:'#3a3939d7'}} />;
    
    const getItemNumber = () => {
      if (groupIndex === undefined || itemIndex === undefined) return '';
      return parentNumber || '';
    };

    return (
      <li
        className={classNames(
          styles.Wrapper,
          clone && styles.clone,
          ghost && styles.ghost,
          indicator && styles.indicator,
          disableSelection && styles.disableSelection,
          disableInteraction && styles.disableInteraction,
          isEditing && styles.editing,
          isExpanded && styles.expanded
        )}
        ref={wrapperRef}
        style={
          {
            '--spacing': `${indentationWidth * depth}px`,
          } as React.CSSProperties
        }
        {...props}
      >
        <div className={styles.TreeItem} ref={ref} style={style}>
          <Handle {...handleProps} />
          {isEditing ? (
            <div className={styles.editForm}>
              <Input.TextArea
                value={editValue}
                onChange={(e) => setEditValue(e.target.value)}
                placeholder="请输入标题"
                className={styles.titleInput}
                autoSize={{ minRows: 1, maxRows: 6 }}
              />
              <div className={styles.editActions}>
                <button
                  className={classNames(styles.actionButton, styles.saveButton)}
                  onClick={handleSave}
                  type="button"
                  title="保存"
                >
                  <SaveOutlined />
                </button>
                <button
                  className={classNames(styles.actionButton, styles.cancelButton)}
                  onClick={handleCancel}
                  type="button"
                  title="取消"
                >
                  <CloseOutlined />
                </button>
              </div>
            </div>
          ) : (
            <>
              <div className={styles.textContent} ref={contentRef}>
                <span className={styles.itemNumber}>{getItemNumber()}</span>
                <span className={styles.text} style={{ maxWidth: isExpanded ? 'none' : `${maxWidth}px` }}>{value}</span>
                {pageSize !== 0 && (
                    <div className={styles.pageSize}>
                      P{pageSize}
                    </div>
                  )}
                {value && value.length > 50 && (
                  <button
                    className={classNames(styles.actionButton, styles.expandButton)}
                    onClick={handleExpand}
                    type="button"
                    title={isExpanded ? "收起" : "展开"}
                  >
                    {isExpanded ? <CompressOutlined /> : <ExpandOutlined />}
                  </button>
                )}
              </div>
              {!clone && (
                <div className={styles.actions}>
                  <button 
                    className={styles.actionButton} 
                    onClick={handleEditClick}
                    type="button"
                    title="编辑"
                  >
                    <EditOutlined />
                  </button>
                  <button
                    className={classNames(styles.actionButton)}
                    onClick={handleCopy}
                    type="button"
                    title="复制"
                  >
                    <CopyOutlined />
                  </button>
                  {onRemove && (
                    <button 
                      className={styles.actionButton} 
                      onClick={onRemove}
                      type="button"
                      title="删除"
                    >
                      <DeleteOutlined />
                    </button>
                  )}
                </div>
              )}
            </>
          )}
          {clone && childCount && childCount > 1 ? (
            <span className={styles.count}>{childCount}</span>
          ) : null}
          {onCollapse && (
            <Action onClick={onCollapse}>
              {collapseIcon}
            </Action>
          )}
        </div>
      </li>
    );
  }
);




