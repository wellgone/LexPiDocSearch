import request from "@/utils/request";

// 获取所有统计数据
export async function getStatistics() {
  return request('/api/statistics', {
    method: 'GET',
  });
}

// 获取书籍总数
export async function getBookCount() {
  return request('/api/statistics/book/count', {
    method: 'GET',
  });
}

// 获取已索引书籍数量
export async function getIndexedBookCount() {
  return request('/api/statistics/book/indexed/count', {
    method: 'GET',
  });
}

// 获取检索报告数量
export async function getReportCount() {
  return request('/api/statistics/report/count', {
    method: 'GET',
  });
}

// 获取笔记数量
export async function getNoteCount() {
  return request('/api/statistics/note/count', {
    method: 'GET',
  });
}

// 获取按分类统计的书籍数量
export async function getBookCountByCategory() {
  return request('/api/statistics/book/category/count', {
    method: 'GET',
  });
}

// 获取按丛书名统计的书籍数量
export async function getBookCountBySeries() {
  return request('/api/statistics/book/series/count', {
    method: 'GET',
  });
} 