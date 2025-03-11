import request from "@/utils/request";

// 获取所有统计数据
export async function getStatistics() {
  return request('/api/statistics', {
    method: 'GET',
  });
}

// 获取书籍总数
export async function getDocCount() {
  return request('/api/statistics/doc/count', {
    method: 'GET',
  });
}

// 获取已索引书籍数量
export async function getIndexedDocCount() {
  return request('/api/statistics/doc/indexed/count', {
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
export async function getDocCountByCategory() {
  return request('/api/statistics/doc/category/count', {
    method: 'GET',
  });
}

// 获取按丛书名统计的书籍数量
export async function getDocCountBySeries() {
  return request('/api/statistics/doc/series/count', {
    method: 'GET',
  });
} 