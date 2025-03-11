import { PageContainer } from '@ant-design/pro-layout';
import { Input, Space, Button } from 'antd';
import { createContext, FC, useState } from 'react';
import { history, Outlet } from 'umi';
import { useLocation, useMatch } from '@@/exports';
import { SearchOutlined } from '@ant-design/icons';

export const SearchContext = createContext<string>('');

const tabList = [
  {
    key: 'list',
    tab: '书籍列表',
  },
  {
    key: 'search',
    tab: '搜索',
  },
];

const Book: FC = () => {
  const location = useLocation();
  const match = useMatch(location.pathname);
  const [searchKeyWord, setSearchKeyWord] = useState<string>('');
  const [searchValue, setSearchValue] = useState<string>('');

  const handleTabChange = (key: string) => {
    const url = match?.pathname === '/' ? '' : match?.pathname.substring(0, match.pathname.lastIndexOf('/'));
    switch (key) {
      case 'list':
        history.push(`${url}/list`);
        break;
      case 'search':
        history.push(`${url}/search`);
        break;
      default:
        history.push(`${url}/list`);
        break;
    }
  };

  const handleSearch = () => {
    if (searchValue.trim()) {
      setSearchKeyWord(searchValue.trim());
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(e.target.value);
    if (!e.target.value.trim()) {
      setSearchKeyWord('');
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const getTabKey = () => {
    const tabKey = location.pathname.substring(location.pathname.lastIndexOf('/') + 1);
    if (tabKey && tabKey !== '/') {
      return tabKey;
    }
    return 'list';
  };

  return (
    <SearchContext.Provider value={searchKeyWord}>
      <PageContainer
        content={
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <Space size="large">
              <Input
                placeholder="请输入搜索关键词"
                value={searchValue}
                onChange={handleInputChange}
                onKeyPress={handleKeyPress}
                style={{
                  width: '500px',
                  height: '40px',
                  fontSize: '16px',
                  borderRadius: '20px',
                  paddingLeft: '20px',
                }}
                allowClear
              />
              <Button
                type="primary"
                icon={<SearchOutlined />}
                onClick={handleSearch}
                style={{
                  height: '40px',
                  width: '100px',
                  borderRadius: '20px',
                  fontSize: '16px',
                }}
              >
                搜索
              </Button>
            </Space>
          </div>
        }
        tabList={tabList}
        tabActiveKey={getTabKey()}
        onTabChange={handleTabChange}
      >
        <Outlet />
      </PageContainer>
    </SearchContext.Provider>
  );
};