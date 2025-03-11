import request from '@/utils/request';
import type { Params, SearchResponse, ListItemDataType } from './data.d';

export async function searchDocs(
  params: Params,
): Promise<SearchResponse<ListItemDataType>> {
  return request('/api/doc/search', {
    params: {
      keyword: params.keyword,
      page: params.pageNum,
      size: params.pageSize
    },
  });
}

export async function queryDocList(
  params: Params,
): Promise<SearchResponse<ListItemDataType>> {
  return request('/api/doc/list', {
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
  return request('/api/doc/search/template', {
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
  return request('/api/doc/search/template', {
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
  return request('/api/doc/search/advanced', {
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
  return request<{ code: number; data: string; message: string }>(`/api/doc/preview/${id}`, {
    method: 'GET',
  });
}

export async function getDocCover(id: string) {
  return request<{ code: number; data: string; message: string }>(`/api/doc/cover/${id}`, {
    method: 'GET',
  });
}

export async function getDocCoverByImgId(id: string) {
  return request<{ code: number; data: string; message: string }>(`/api/doc/cover/img/${id}`, {
    method: 'GET',
  });
}

/** 获取所有非空类目 */
export async function getCategories() {
  return request<SearchResponse<string[]>>('/api/doc/categories', {
    method: 'GET',
  });
}
