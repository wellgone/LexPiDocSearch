import {
  LikeOutlined,
  LoadingOutlined,
  StarOutlined,
  DownOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  SearchOutlined,
  UserOutlined,
  BookOutlined,
  CalendarOutlined,
  BarcodeOutlined,
  ApiOutlined,
  TrophyOutlined,
  FilePdfOutlined,
} from '@ant-design/icons';
import { Button, Card, Col, Form, List, Row, Select, Input, DatePicker, Space, Tag, Dropdown, Menu, Tooltip, Empty, Popconfirm, Image, message } from 'antd';
import { FC, useEffect, useState, useCallback } from 'react';
import { useLocation, useNavigate } from '@umijs/max';
import { debounce } from 'lodash';
import ArticleListContent from './components/ArticleListContent';
import TagSelect from './components/TagSelect';
import type { ListItemDataType, Params, SearchHistory } from './data.d';
import { searchBooks, queryBookList, getBookCoverByImgId, getCategories } from './service';
import { v4 as uuidv4 } from 'uuid';
import moment from 'moment';
import { getPreviewUrlByFileName } from '@/services/book';
import { PageContainer } from '@ant-design/pro-layout';

const FormItem = Form.Item;

const pageSize = 10;
  
// 排序选项
const sortOptions = [
  { value: 'relevance', label: '相关度' },
  { value: 'publicationYear', label: '出版年份' },
];  
// 类型选项
const typeOptions = [
  { value: 1, label: '图书' },
  { value: 2, label: '自定义PDF' },
];



const SEARCH_HISTORY_KEY = 'book_search_history';
const MAX_HISTORY_ITEMS = 10;

// 高亮文本的辅助函数
const HighlightText: FC<{ text: string | undefined | number; keyword: string }> = ({ text, keyword }) => {
  if (!keyword || !text) return <>{text || ''}</>;
  const parts = String(text).split(new RegExp(`(${keyword})`, 'gi'));
  return (
    <>
      {parts.map((part, i) =>
        part.toLowerCase() === keyword.toLowerCase() ? (
          <span key={i} style={{ color: '#1890ff',  backgroundColor: '#e0f7fa', fontWeight: 'bold' }}>
            {part}
          </span>
        ) : (
          part
        )
      )}
    </>
  );
};

// 修改BookCover组件的类型定义
interface BookCoverProps {
  picUrl?: string | null;
  title: string;
  bookId?: number | null;
}

const BookCover: FC<BookCoverProps> = ({ picUrl = '', title, bookId = null }) => {
  const [base64Data, setBase64Data] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchBookCover = async () => {
      if (picUrl?.startsWith('img/') && bookId) {
        const imgId = picUrl.split('/')[1];
        setLoading(true);
        try {
          const response = await getBookCoverByImgId(imgId);
          if (response.code === 0 && response.data) {
            setBase64Data(response.data);
          }
        } catch (error) {
          console.error('获取图书封面失败:', error);
        } finally {
          setLoading(false);
        }
      }
    };

    fetchBookCover();
  }, [picUrl, bookId]);

  return (
    <div style={{ width: 200, textAlign: 'center' }}>
      {loading ? (
        <div style={{ width: 150, height: 200, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
          <LoadingOutlined style={{ fontSize: 24 }} />
        </div>
      ) : (
        <Image
          width={150}
          height={200}
          src={base64Data 
            ? `data:image/jpeg;base64,${base64Data}` 
            : (picUrl?.startsWith('img/') 
              ? '/api2/2024/11/27/6746e954eecd5.jpg' 
              : (picUrl 
                ? `/api2/${picUrl}` 
                : `data:image/svg+xml,${encodeURIComponent(`
                  <svg width="150" height="200" xmlns="http://www.w3.org/2000/svg">
                    <rect width="100%" height="100%" fill="#f5f5f5"/>
                    <text x="50%" y="50%" font-family="Arial" font-size="18" font-weight="bold" fill="#999" text-anchor="middle" dominant-baseline="middle">
                      暂无封面
                    </text>
                  </svg>
                `)}`
              )
            )}
          alt={title}
          style={{ objectFit: 'cover', border: '1px solid #eee', borderRadius: '4px' }}
        />
      )}
    </div>
  );
};

interface IconTextProps {
  icon: React.FC<{ style?: React.CSSProperties }>;
  text: React.ReactNode;
  key?: string;
}


const IconTip: React.FC<IconTextProps> = ({ icon: Icon, text }) => (
  <Space>
    <Tooltip title={text}>
      <Icon />
    </Tooltip>
  </Space>
);

const BookSearch: FC = () => {
  const [form] = Form.useForm();
  const location = useLocation();
  const navigate = useNavigate();
  const [initLoading, setInitLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [loadMore, setLoadMore] = useState(true);
  const [data, setData] = useState<ListItemDataType[]>([]);
  const [list, setList] = useState<ListItemDataType[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [queryYear, setQueryYear] = useState<number>();
  const [querySource, setQuerySource] = useState<string>();
  const [expanded, setExpanded] = useState(false);
  const [orderBy, setOrderBy] = useState<string>('relevance');
  const [orderType, setOrderType] = useState<'asc' | 'desc'>('desc');
  const [searchHistory, setSearchHistory] = useState<SearchHistory[]>([]);
  const [searchKeyWord, setSearchKeyWord] = useState<string>('');
  const [searchValue, setSearchValue] = useState<string>('');
  const [totalCount, setTotalCount] = useState<number>(0);
  const [categories, setCategories] = useState<{ value: string; label: string }[]>([]);
  const [showCategories, setShowCategories] = useState(false);

  // 从URL参数中获取搜索条件
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const keyword = params.get('keyword');
    const category = params.get('category');
    const year = params.get('year');
    const source = params.get('source');
    const order = params.get('orderBy');
    const type = params.get('orderType') as 'asc' | 'desc';

    if (keyword) {
      setSearchValue(keyword);
      setSearchKeyWord(keyword);
    }
    if (category || year || source) {
      form.setFieldsValue({
        category: category || undefined,
        year: year ? moment(year) : undefined,
        source: source || undefined,
      });
    }
    if (order) setOrderBy(order);
    if (type) setOrderType(type);
  }, [location.search]);

  // 更新URL参数
  const updateUrlParams = useCallback((params: Record<string, any>) => {
    const searchParams = new URLSearchParams(location.search);
    Object.entries(params).forEach(([key, value]) => {
      if (value) {
        searchParams.set(key, value.toString());
      } else {
        searchParams.delete(key);
      }
    });
    navigate(`${location.pathname}?${searchParams.toString()}`);
  }, [location, navigate]);

  // 防抖处理搜索
  const debouncedSearch = useCallback(
    debounce((value: string) => {
      if (value.trim()) {
        setSearchKeyWord(value.trim());
        updateUrlParams({ keyword: value.trim() });
      }
    }, 500),
    [updateUrlParams]
  );

  const handleSearch = () => {
    if (searchValue.trim()) {
      setSearchKeyWord(searchValue.trim());
      updateUrlParams({ keyword: searchValue.trim() });
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchValue(value);
    if (!value.trim()) {
      setSearchKeyWord('');
      updateUrlParams({ keyword: undefined });
    } else {
      debouncedSearch(value);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  // 加载搜索历史
  useEffect(() => {
    const history = localStorage.getItem(SEARCH_HISTORY_KEY);
    if (history) {
      setSearchHistory(JSON.parse(history));
    }
  }, []);

  // 保存搜索历史
  const saveSearchHistory = (keyword: string) => {
    if (!keyword.trim()) return;

    const currentFilters = form.getFieldsValue();
    const newHistory: SearchHistory = {
      id: uuidv4(),
      keyword: keyword.trim(),
      timestamp: Date.now(),
      category: currentFilters.category,
      filters: {
        author: currentFilters.author,
        publisher: currentFilters.publisher,
        year: currentFilters.year?.$y,
        source: currentFilters.source,
      },
    };

    // 检查是否已存在相同关键词的记录
    const existingIndex = searchHistory.findIndex(item => item.keyword === keyword.trim());
    let updatedHistory: SearchHistory[];

    if (existingIndex !== -1) {
      // 如果存在，删除旧记录，将新记录放在最前面
      updatedHistory = [
        newHistory,
        ...searchHistory.slice(0, existingIndex),
        ...searchHistory.slice(existingIndex + 1)
      ].slice(0, MAX_HISTORY_ITEMS);
    } else {
      // 如果不存在，直接添加到最前面
      updatedHistory = [newHistory, ...searchHistory].slice(0, MAX_HISTORY_ITEMS);
    }
    
    setSearchHistory(updatedHistory);
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(updatedHistory));
  };

  // 清除搜索历史
  const clearSearchHistory = () => {
    setSearchHistory([]);
    localStorage.removeItem(SEARCH_HISTORY_KEY);
  };

  // 删除单条搜索历史
  const deleteSearchHistory = (id: string) => {
    const updatedHistory = searchHistory.filter(item => item.id !== id);
    setSearchHistory(updatedHistory);
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(updatedHistory));
  };

  // 使用历史搜索条件
  const applySearchHistory = (history: SearchHistory) => {
    // 设置表单值
    form.setFieldsValue({
      category: history.category,
      ...history.filters,
    });
    
    // 设置搜索值并触发搜索
    setSearchValue(history.keyword);
    setSearchKeyWord(history.keyword);
    
    // 将当前记录移到最前面
    const updatedHistory = [
      history,
      ...searchHistory.filter(item => item.id !== history.id)
    ];
    setSearchHistory(updatedHistory);
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(updatedHistory));
  };

  // 提取fetchData函数到组件级别，以便复用
  const fetchData = async () => {
    setInitLoading(true);
    try {
      const currentFilters = form.getFieldsValue();
      const params: Params = {
        pageSize,
        pageNum: currentPage,
        publicationYear: queryYear,
        source: querySource,
        orderBy,
        orderType,
        category: currentFilters.category?.join(','),
        author: currentFilters.author,
        publisher: currentFilters.publisher,
        type: currentFilters.type,
      };
      
      const response = await (searchKeyWord 
        ? searchBooks({ ...params, keyword: searchKeyWord }) 
        : queryBookList({ ...params, title: searchKeyWord }));
      setData(response.data.records);
      setList(response.data.records);
      setTotalCount(response.data.total);
      setLoadMore(response.data.records.length === pageSize);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setInitLoading(false);
    }
  };

  // 首次加载和搜索条件变化时加载数据
  useEffect(() => {
    setCurrentPage(1);
    // 只有当搜索关键词变化时才保存历史记录
    if (searchKeyWord && searchHistory.every(item => item.keyword !== searchKeyWord)) {
      saveSearchHistory(searchKeyWord);
    }
    fetchData();
  }, [searchKeyWord, queryYear, querySource, orderBy, orderType]);

  const onLoadMore = async () => {
    setLoading(true);
    setCurrentPage(currentPage + 1);

    try {
      const params: Params = {
        pageSize,
        pageNum: currentPage + 1,
        publicationYear: queryYear,
        source: querySource,
        orderBy,
        orderType,
      };

      const response = await (searchKeyWord 
        ? searchBooks({ ...params, keyword: searchKeyWord }) 
        : queryBookList({ ...params, title: searchKeyWord }));
      const newRecords = response.data.records;

      if (newRecords.length === 0) {
        setLoadMore(false);
      } else {
        setData([...data, ...newRecords]);
        setList([...data, ...newRecords]);
        setLoadMore(newRecords.length === pageSize);
      }
    } catch (error) {
      console.error('Failed to load more:', error);
    } finally {
      setLoading(false);
    }
  };

  // 优化表单值变化处理
  const onFromValuesChange = (changedValues: Record<string, any>) => {
    const updates: Record<string, any> = {};
    
    if ('category' in changedValues) {
      // 类目选择不再更新 URL 参数
      const selectedCategories = changedValues.category || [];
      // 直接调用 fetchData，不更新 URL
      setCurrentPage(1);
      fetchData();
      return; // 直接返回，不执行后续的 URL 更新
    }
    
    if ('year' in changedValues) {
      setQueryYear(changedValues.year?.$y);
      updates.year = changedValues.year?.$y;
    }
    if ('source' in changedValues) {
      setQuerySource(changedValues.source);
      updates.source = changedValues.source;
    }
    if ('type' in changedValues) {
      updates.type = changedValues.type;
    }
    
    // 只有非类目相关的参数才更新 URL
    updateUrlParams(updates);
    setCurrentPage(1);
    fetchData();
  };

  // 优化排序处理
  const handleSortChange = (value: string) => {
    setOrderBy(value);
    updateUrlParams({ orderBy: value });
  };

  const handleOrderTypeChange = (type: 'asc' | 'desc') => {
    setOrderType(type);
    updateUrlParams({ orderType: type });
  };

  // 加载类目数据
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await getCategories();
        if (response.code === 0 && response.data) {
          // 对类目进行排序，将"其他"放在最后
          const sortedCategories = response.data.sort((a: string, b: string) => {
            if (a === '其他') return 1;
            if (b === '其他') return -1;
            return a.localeCompare(b);
          });

          const categoryList = sortedCategories.map((cat: string) => ({
            value: cat,
            label: cat
          }));
          
          setCategories(categoryList);
          setShowCategories(categoryList.length > 0);
        }
      } catch (error) {
        console.error('获取类目失败:', error);
        setShowCategories(false);
      }
    };

    fetchCategories();
  }, []);

  const loadMoreDom = list.length > 0 && (
    <div style={{ textAlign: 'center', marginTop: 16 }}>
      {!loadMore ? (
        <span>没有更多</span>
      ) : (
        <Button onClick={onLoadMore} style={{ paddingLeft: 48, paddingRight: 48 }}>
          {loading ? (
            <span>
              <LoadingOutlined /> 加载中...
            </span>
          ) : (
            '加载更多'
          )}
        </Button>
      )}
    </div>
  );

  const sortMenu = (
    <Menu 
      selectedKeys={[orderBy]}
      onClick={({ key }) => handleSortChange(key)}
    >
      {sortOptions.map(option => (
        <Menu.Item key={option.value}>
          {option.label}
          {orderBy === option.value && (
            <Button 
              type="link" 
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                handleOrderTypeChange(orderType === 'asc' ? 'desc' : 'asc');
              }}
              icon={orderType === 'asc' ? <DownOutlined rotate={180} /> : <DownOutlined />}
            />
          )}
        </Menu.Item>
      ))}
    </Menu>
  );

  const handlePreview = async (filename: string) => {
    try {
      const response = await getPreviewUrlByFileName(filename);
      if (response.code === 0 && response.data) {
        window.open('/api3/'+response.data, '_blank');
      } else {
        message.error(response.message || '获取预览地址失败');
      }
    } catch (error) {
      message.error('获取预览地址失败');
    }
  };

  return (
    <PageContainer>
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
      <Card bordered={false}>
        <Form
          form={form}
          onValuesChange={onFromValuesChange}
          layout="vertical"
          initialValues={{
            source: '不限',
          }}
        >
          <div style={{ marginBottom: 16 }}>
            <Row justify="space-between" align="middle">
              {showCategories && (
                <Col flex="auto">
                  <span style={{ fontSize: 14, marginRight: 8 }}>所属类目：</span>
                  <Form.Item 
                    name="category" 
                    noStyle 
                    initialValue={[]}
                  >
                    <TagSelect 
                      multiple 
                      hideCheckAll
                      style={{ 
                        '--tag-select-option-color': '#1890ff',
                        '--tag-select-option-bg': '#e6f7ff',
                        '--tag-select-option-border-color': '#91d5ff',
                      } as React.CSSProperties}
                    >
                      {categories.map((cat) => (
                        <TagSelect.Option 
                          key={cat.value} 
                          value={cat.value}
                          style={{
                            padding: '4px 12px',
                            borderRadius: '16px',
                            fontSize: '14px',
                            marginRight: '8px',
                            marginBottom: '8px',
                            transition: 'all 0.3s',
                            ...(cat.value === 'all' ? { 
                              fontWeight: 'bold',
                              backgroundColor: form.getFieldValue('category')?.length === categories.length - 1 ? 
                                'var(--tag-select-option-bg)' : 'transparent'
                            } : {}),
                          }}
                        >
                          {cat.label}
                        </TagSelect.Option>
                      ))}
                    </TagSelect>
                  </Form.Item>
                </Col>
              )}
              <Col flex="none" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <Dropdown overlay={sortMenu} trigger={['click']}>
                  <Button>
                    {sortOptions.find((opt) => opt.value === orderBy)?.label} <DownOutlined />
                  </Button>
                </Dropdown>
                <a
                  style={{ color: '#1890ff', cursor: 'pointer' }}
                  onClick={() => setExpanded(!expanded)}
                >
                  {expanded ? '收起筛选' : '展开筛选'}{' '}
                  <DownOutlined style={{ transform: expanded ? 'rotate(180deg)' : 'none' }} />
                </a>
              </Col>
            </Row>
          </div>

          <div style={{ marginBottom: expanded ? 16 : 0 }}>
            <Row gutter={24} style={{ display: expanded ? 'flex' : 'none' }}>
              <Col span={6}>
                <FormItem label="出版社：" name="publisher">
                  <Input placeholder="不限" allowClear />
                </FormItem>
              </Col>
              <Col span={6}>
                <FormItem label="作者：" name="author">
                  <Input placeholder="不限" allowClear />
                </FormItem>
              </Col>
              <Col span={6}>
                <FormItem label="出版年份：" name="year">
                  <DatePicker picker="year" placeholder="不限" style={{ width: '100%' }} />
                </FormItem>
              </Col>
              <Col span={6}>
                <FormItem name="type" label="类型">
                  <Select
                    placeholder="请选择类型"
                    allowClear
                    options={typeOptions}
                  />
                </FormItem>
              </Col>
            </Row>
          </div>
        </Form>

        {/* 搜索历史 */}
        {searchHistory.length > 0 && (
          <div style={{ marginTop: 16 }}>
            <Row justify="space-between" align="middle" style={{ marginBottom: 8 }}>
              <Col>
                <ClockCircleOutlined /> 搜索历史
              </Col>
              <Col>
                <Popconfirm
                  title="确定要清除所有搜索历史吗？"
                  onConfirm={clearSearchHistory}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button type="link" size="small">
                    清除历史
                  </Button>
                </Popconfirm>
              </Col>
            </Row>
            <Space wrap>
              {searchHistory.map((history) => (
                <Tag
                  key={history.id}
                  closable
                  onClose={(e) => {
                    e.stopPropagation();
                    deleteSearchHistory(history.id);
                  }}
                  style={{ cursor: 'pointer' }}
                  onClick={() => applySearchHistory(history)}
                >
                  {history.keyword}
                </Tag>
              ))}
            </Space>
          </div>
        )}
      </Card>

      <Card
        style={{ marginTop: 24 }}
        bordered={false}
        bodyStyle={{ padding: '8px 32px 32px 32px' }}
      >
        {searchKeyWord && (
          <div style={{fontSize: '14px', display: 'flex', justifyContent: 'flex-end', marginBottom: 16, color: '#666' }}>
            找到 <span style={{ color: '#1890ff', fontWeight: 'bold' }}>{totalCount}</span> 条与
            &ldquo;<span style={{ color: '#1890ff', fontWeight: 'bold' }}>{searchKeyWord}</span>&rdquo;
            相关的内容
          </div>
        )}
        <List<ListItemDataType>
          size="large"
          loading={initLoading}
          rowKey="id"
          itemLayout="vertical"
          loadMore={loadMoreDom}
          dataSource={list}
          locale={{
            emptyText: <Empty description="暂无数据" />,
          }}
          renderItem={(item) => (
            <List.Item
              key={item.id}
              // actions={[
              //   <Tag key="list-vertical-type" color={item.type === 1 ? 'blue' : 'green'}>
              //     {item.type === 1 ? '图书' : '自定义PDF'}
              //   </Tag>,
              // ]}
              extra={
                <BookCover 
                  picUrl={item.picUrl || null} 
                  title={item.title} 
                  bookId={item.id || null} 
                />
              }
            >
              <List.Item.Meta
                title={
                  <Space direction="vertical" size={2} style={{ width: '100%' }}>
                    {/* 标题 */}
                    <a
                      onClick={() => {
                        if (item.fileName) {
                          handlePreview(item.fileName);
                        } else {
                          message.warning('暂不可阅读：请先上传！');
                        }
                      }}
                      style={{
                        fontSize: '22px',
                        fontWeight: '550',
                        cursor: 'pointer',
                        color: item.type !== 1 ? '#A020F0' : undefined,
                      }}
                    >
                      <IconTip icon={item.type === 1 ? BookOutlined : FilePdfOutlined} text={item.type === 1 ? '图书' : '自定义PDF'} />
                      <HighlightText text={item.title} keyword={searchKeyWord} />
                    </a>
                  </Space>
                }
                description={
                  <>
                    <Space split="" size={16}>
                      {[
                        { icon: <UserOutlined />, label: '作者', value: item.author },
                        { icon: <BookOutlined />, label: '出版社', value: item.publisher },
                        { icon: <CalendarOutlined />, label: '出版时间', value: item.publicationYear },
                        { icon: <BarcodeOutlined />, label: 'ISBN', value: item.isbn },
                        { icon: <ApiOutlined />, label: '来源', value: item.source },
                        { icon: <TrophyOutlined />, label: '评分', value: item.score },
                      ]
                      .filter(({ value }) => value) // 仅渲染值不为空的项
                      .map(({ icon, label, value }) => (
                        <Tooltip key={label} title={label}>
                          <span style={{ fontSize: '15px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                            {icon}
                            <HighlightText text={value} keyword={searchKeyWord} />
                          </span>
                        </Tooltip>
                      ))}
                    </Space>
                  </>
                }
              />
              <ArticleListContent 
                data={{
                  ...item,
                  content: item.content || '',
                }} 
              />
            </List.Item>
          )}
        />
      </Card>
    </PageContainer>
  );
};

export default BookSearch;
