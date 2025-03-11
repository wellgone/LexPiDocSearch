import React from 'react';
import { usePagination } from 'react-instantsearch';
import { Pagination as AntPagination } from 'antd';

const CustomPagination = (props) => {
  const {
    pages,
    currentRefinement,
    nbPages,
    isFirstPage,
    isLastPage,
    refine,
  } = usePagination(props);

  const handlePageChange = (page: number) => {
    refine(page - 1); // Ant Design 的页码从 1 开始，Algolia 的页码从 0 开始
  };

  return (
    <AntPagination
      size="small"
      current={currentRefinement + 1} // Ant Design 的 current 从 1 开始
      total={nbPages * 10} // 假设每页显示 10 条数据
      pageSize={10}
      onChange={handlePageChange}
      showSizeChanger={false}
      showQuickJumper
      disabled={isFirstPage && isLastPage} // 如果只有一页则禁用
      hideOnSinglePage={nbPages <= 1} // 如果只有一页则隐藏分页
    />
  );
};

export default CustomPagination; 