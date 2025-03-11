import { useStats } from 'react-instantsearch';
import { Space, Typography } from 'antd';
import { SearchOutlined, ClockCircleOutlined } from '@ant-design/icons';

const { Text } = Typography;

function CustomStats() {
  const { nbHits, processingTimeMS, query } = useStats();

  // 如果没有查询条件且没有结果，则不显示统计信息
  if (!query && nbHits === 0) {
    return null;
  }

  return (
    <div style={{ padding: '4px 8px', fontSize: 14 }}>
      <Space size={16}>
        <Space size={4}>
          <SearchOutlined style={{ color: '#1890ff' }} />
          <Text>
            找到 <Text>{nbHits.toLocaleString()}</Text> 条结果
          </Text>
        </Space>
        <Space size={4}>
          <ClockCircleOutlined style={{ color: processingTimeMS > 500 ? 'red' : '#52c41a' }} />
          <Text>
            耗时 <Text>{processingTimeMS.toLocaleString()}</Text> 毫秒
          </Text>
        </Space>
      </Space>
    </div>
  );
}

export default CustomStats; 