import React, { useEffect, useState } from 'react';
import { Tag } from 'antd';
import { DownOutlined } from '@ant-design/icons';
import type { TagSelectProps, TagSelectOptionProps } from '../../data.d';
import classNames from 'classnames';

const { CheckableTag } = Tag;

const TagSelectOption: React.FC<TagSelectOptionProps> = ({ children, checked = false, onChange, value, style }) => (
  <CheckableTag
    checked={checked}
    style={{
      ...style,
      backgroundColor: checked ? 'var(--tag-select-option-bg, #e6f7ff)' : 'transparent',
      color: checked ? 'var(--tag-select-option-color, #1890ff)' : 'rgba(0, 0, 0, 0.85)',
      borderColor: checked ? 'var(--tag-select-option-border-color, #91d5ff)' : '#d9d9d9',
    }}
    onChange={(state) => onChange?.(value, state)}
  >
    {children}
  </CheckableTag>
);

const TagSelect: React.FC<TagSelectProps> & { Option: typeof TagSelectOption } = ({
  expandable,
  value,
  defaultValue = [],
  style,
  actionsText = {},
  onChange,
  onExpand,
  children,
}) => {
  const [expand, setExpand] = useState(false);
  const [selected, setSelected] = useState<string[]>(
    Array.isArray(value) ? value.map(String) : defaultValue.map(String)
  );

  useEffect(() => {
    if (Array.isArray(value)) {
      setSelected(value.map(String));
    }
  }, [value]);

  const handleTagChange = (tagValue: string | number, checked: boolean) => {
    const nextSelected = checked
      ? [...selected, String(tagValue)]
      : selected.filter((t) => t !== String(tagValue));
    setSelected(nextSelected);
    onChange?.(nextSelected);
  };

  const handleExpand = () => {
    setExpand(!expand);
    onExpand?.(!expand);
  };

  const isTagSelected = (tagValue: string | number) => selected.includes(String(tagValue));

  return (
    <div 
      className={classNames('tag-select', {
        'tag-select-expanded': expand,
        'tag-select-has-expand': expandable,
      })} 
      style={style}
    >
      <div className="tag-select-content">
        {React.Children.map(children, (child) => {
          if (!React.isValidElement(child)) return null;
          const childProps = child.props as TagSelectOptionProps;
          return React.cloneElement(child as React.ReactElement<TagSelectOptionProps>, {
            key: childProps.value,
            value: childProps.value,
            checked: isTagSelected(childProps.value),
            onChange: handleTagChange,
            style: childProps.style,
          });
        })}
      </div>
      {expandable && (
        <a className="tag-select-trigger" onClick={handleExpand}>
          {expand ? actionsText.collapseText || '收起' : actionsText.expandText || '展开'}{' '}
          <DownOutlined rotate={expand ? 180 : 0} />
        </a>
      )}
    </div>
  );
};

TagSelect.Option = TagSelectOption;

export default TagSelect;
