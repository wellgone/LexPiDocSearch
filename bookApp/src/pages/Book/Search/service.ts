import request from '@/utils/request';
import type { Params, SearchResponse, ListItemDataType } from './data.d';

export async function searchBooks(
  params: Params,
): Promise<SearchResponse<ListItemDataType>> {
  return request('/api/book/search', {
    params: {
      keyword: params.keyword,
      page: params.pageNum,
      size: params.pageSize
    },
  });
}

export async function queryBookList(
  params: Params,
): Promise<SearchResponse<ListItemDataType>> {
  return request('/api/book/list', {
    params: {
      current: params.pageNum,
      pageSize: params.pageSize,
      title: params.title,
      author: params.author,
      publisher: params.publisher,
      publicationYear: params.publicationYear,
      source: params.source,
      category: params.category,
      type: params.type,
    },
  });
}

export async function queryAuthorList(
  params: Params,
): Promise<{ data: string[] }> {
  return request('/api/book/search/template', {
    params: {
      field: 'author',
      value: params.keyword || '',
      page: 1,
      size: 10,
    },
  });
}

export async function queryPublisherList(
  params: Params,
): Promise<{ data: string[] }> {
  return request('/api/book/search/template', {
    params: {
      field: 'publisher',
      value: params.keyword || '',
      page: 1,
      size: 10,
    },
  });
}

export async function advancedSearch(
  params: Params,
): Promise<SearchResponse<ListItemDataType>> {
  return request('/api/book/search/advanced', {
    params: {
      keyword: params.keyword,
      category: params.category,
      author: params.author,
      yearFrom: params.yearFrom,
      yearTo: params.yearTo,
      page: params.pageNum,
      size: params.pageSize
    },
  });
}

export async function getPreviewUrl(id: number) {
  return request<{ code: number; data: string; message: string }>(`/api/book/preview/${id}`, {
    method: 'GET',
  });
}

export async function getBookCover(id: string) {
  return request<{ code: number; data: string; message: string }>(`/api/book/cover/${id}`, {
    method: 'GET',
  });
}

export async function getBookCoverByImgId(id: string) {
  return request<{ code: number; data: string; message: string }>(`/api/book/cover/img/${id}`, {
    method: 'GET',
  });
}

/** 获取所有非空类目 */
export async function getCategories() {
  return request<SearchResponse<string[]>>('/api/book/categories', {
    method: 'GET',
  });
}
