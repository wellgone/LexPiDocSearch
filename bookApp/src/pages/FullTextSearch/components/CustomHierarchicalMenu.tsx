import { useHierarchicalMenu } from 'react-instantsearch';
import { Tree, Input, Space, Tag } from 'antd';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import type { DataNode } from 'antd/es/tree';
import { useState } from 'react';

interface CustomHierarchicalMenuProps {
  attributes: string[];
  limit?: number;
  showMore?: boolean;
  showMoreLimit?: number;
  separator?: string;
}

const CustomHierarchicalMenu = (props: CustomHierarchicalMenuProps) => {
  const {
    items,
    refine,
    canToggleShowMore,
    isShowingMore,
    toggleShowMore,
  } = useHierarchicalMenu(props);

  const [searchValue, setSearchValue] = useState('');
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);

  // 将 items 转换为 Ant Design Tree 所需的数据结构
  const convertToTreeData = (items: any[]): DataNode[] => {
    return items.map((item) => ({
      key: item.value,
      title: (
        <span style={{ color: '#000000', fontSize: 14, display: 'flex', alignItems: 'center' }}>
          {item.label}
          <Tag color="default" style={{ marginLeft: 8 }}>
            {item.count}
          </Tag>
        </span>
      ),
      children: item.data ? convertToTreeData(item.data) : undefined,
      isLeaf: !item.data || item.data.length === 0,
    }));
  };

  const treeData = convertToTreeData(items);

  // 处理树节点选择
  const handleSelect = (newSelectedKeys: React.Key[], info: any) => {
    setSelectedKeys(newSelectedKeys);
    refine(info.node.key);
  };

  // 搜索过滤树节点
  const filterTreeNode = (node: any) => {
    const title = node.title?.props?.children[0]?.toLowerCase() || '';
    return title.indexOf(searchValue.toLowerCase()) >= 0;
  };

  return (
    <div style={{ minHeight: 210 }}>
      {/* <div style={{ marginBottom: 10 }}>
        <Input
          placeholder="搜索主题"
          prefix={<SearchOutlined style={{ fontSize: 16 }} />}
          onChange={(e) => setSearchValue(e.target.value)}
          style={{ marginBottom: 8 }}
          allowClear
        />
      </div> */}
      <div>
        <Tree
          treeData={treeData}
          onSelect={handleSelect}
          selectedKeys={selectedKeys}
          filterTreeNode={filterTreeNode}
          showLine={{ showLeafIcon: false }}
          switcherIcon={<DownOutlined />}
          defaultExpandAll
          virtual={false}
          titleRender={(nodeData: any) => (
            <div style={{
              padding: '4px 0',
              backgroundColor: selectedKeys.includes(nodeData.key) 
                ? '#e6f4ff'
                : 'transparent',
              borderRadius: '4px',
            }}>
              {nodeData.title}
            </div>
          )}
        />
      </div>
      {canToggleShowMore && (
        <Space style={{ marginTop: 8 }}>
          <a onClick={toggleShowMore}>
            {isShowingMore ? '收起' : '展开更多'}
          </a>
        </Space>
      )}
    </div>
  );
};

export default CustomHierarchicalMenu; 