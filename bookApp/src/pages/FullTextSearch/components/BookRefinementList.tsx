import { useRefinementList } from 'react-instantsearch';
import { useEffect, useState } from 'react';
import { Input, PaginationProps, Space, Tag, Checkbox } from 'antd';
import { Pagination } from 'antd';
import BookOutlined from '@ant-design/icons/BookOutlined';

function BookRefinementList(props: any) {
  const {
    items,
    refine,
    searchForItems,
    toggleShowMore,
  } = useRefinementList(props);


  const [currentPage, setCurrentPage] = useState(1); // 初始页码
  const [pagSize, setPagSize] = useState(15); // 初始页码

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  //组件一旦形成，则设置最多列
  useEffect(() => {
    toggleShowMore(true);
  }, []);

  const onShowSizeChange: PaginationProps['onShowSizeChange'] = (current, pageSize) => {
    setCurrentPage(current);
    setPagSize(pageSize);
  };

  const truncateLabel = (label: string, maxLength: number) => {
    return label.length > maxLength ? label.slice(0, maxLength) + '...' : label;
  };

  return (
    <div style={{minHeight:730}}>
      <div style={{marginBottom:10, fontSize:16}}>
        <Input
          placeholder="在结果中检索书籍名称"
          prefix={<BookOutlined />}
          type="search"
          autoComplete="off"
          autoCorrect="off"
          autoCapitalize="off"
          spellCheck={false}
          maxLength={512}
          onChange={(event) => searchForItems(event.currentTarget.value)}
        />
      </div>
      <div style={{maxHeight:650, fontSize:16, overflowY: 'auto', overflowX: 'hidden'}}>
        <ul style={{padding:0}}>
          <Space direction="vertical" style={{ width: '100%' }}>
            {items.slice((currentPage - 1) * pagSize, currentPage * pagSize).map((item) => (
              <li key={item.label} style={{display:'flex',alignItems:'center'}}>
                <Checkbox
                  checked={item.isRefined}
                  onChange={() => refine(item.value)}
                  style={{ borderRadius: '10%' }}
                >
                  <span style={{ fontSize: 14, lineHeight: 0.5 }}>{truncateLabel(item.label, 38)}</span>
                  <Tag color="default" style={{ marginLeft: 8 }}>
                    {item.count}
                  </Tag>
                </Checkbox>
              </li>
            ))}
          </Space>
        </ul>
      </div>
      <div style={{margin:16,justifyContent: 'center', display: 'flex'}}>
        <Pagination
          size="small"
          defaultCurrent={1}
          defaultPageSize={15}
          total={items.length}
          onChange={handlePageChange}
          onShowSizeChange={onShowSizeChange}
          pageSizeOptions={['10', '15','20', '25']}
        ></Pagination>
      </div>
    </div>
  );
}

export default BookRefinementList;