import request from "@/utils/request";

export interface LpNote {
  id: number;
  content: string;
  sourceName: string;
  sourcePress: string;
  sourceAuthor: string;
  sourcePublicationDate: string;
  sourcePageSize: number;
  createTime: string;
  modifiedTime: string;
  userId: string;
  sourceUrl: string;
  tags: string;
  parentId: number | null;
  orderNum: number;
  children?: LpNote[];
}

export interface LpSearchReport {
  id: number;
  title: string;
  type: string;
  searchSubject: number;
  createTime: string;
  modifiedTime: string;
  userId: string;
}

export interface BaseResponse<T> {
  code: number;
  data: T;
  message: string;
}

// 笔记相关API
export async function getNoteList(params: {
  current: number;
  pageSize: number;
  keyword?: string;
}) {
  return request<BaseResponse<LpNote[]>>('/api/note/list', {
    method: 'GET',
    params,
  });
}

export async function createNote(data: Partial<LpNote>) {
  return request<BaseResponse<boolean>>('/api/note/create', {
    method: 'POST',
    data,
  });
}

export async function updateNote(data: Partial<LpNote>) {
  return request<BaseResponse<boolean>>('/api/note/update', {
    method: 'PUT',
    data,
  });
}

/**
 * 删除笔记
 */
export async function deleteNote(id: number) {
  return request<BaseResponse<boolean>>(`/api/note/delete/${id}`, {
    method: 'DELETE',
  });
}

/**
 * 批量删除笔记
 */
export async function batchDeleteNotes(noteIds: number[]) {
  return request<BaseResponse<boolean>>('/api/note/batch/delete', {
    method: 'DELETE',
    data: { noteIds },
  });
}

// 检索报告相关API
export async function getReportList(params: {
  current: number;
  pageSize: number;
  keyword?: string;
}) {
  return request<BaseResponse<LpSearchReport[]>>('/api/report/list', {
    method: 'GET',
    params,
  });
}

export async function createReport(data: Partial<LpSearchReport>) {
  return request<BaseResponse<boolean>>('api/report/create', {
    method: 'POST',
    data,
  });
}

export async function updateReport(data: Partial<LpSearchReport>) {
  return request<BaseResponse<boolean>>('/api/report/update', {
    method: 'PUT',
    data,
  });
}

export async function deleteReport(id: number) {
  return request<BaseResponse<boolean>>(`/api/report/delete/${id}`, {
    method: 'DELETE',
  });
}

export async function getReportNotes(reportId: number) {
  return request<BaseResponse<LpNote[]>>(`/api/report/${reportId}/notes`, {
    method: 'GET',
  });
}

export async function exportReportToPdf(reportId: number) {
  return request<Blob>(`/api/report/${reportId}/export/pdf`, {
    method: 'GET',
    responseType: 'blob',
  });
}

export async function exportReportToMarkdown(reportId: number) {
  return request<string>(`/api/report/${reportId}/export/markdown`, {
    method: 'GET',
  });
}

export async function generateShareLink(reportId: number) {
  return request<BaseResponse<string>>(`/api/report/${reportId}/share`, {
    method: 'POST',
  });
}

// 获取用户的检索报告列表
export async function getUserReports(userId: string) {
  return request<BaseResponse<LpSearchReport[]>>(`api/report/user/${userId}`, {
    method: 'GET',
  });
}

// 更新笔记层级关系
export async function updateNoteHierarchy(data: { noteId: number; parentId: number | null; orderNum: number }) {
  return request<BaseResponse<boolean>>('/api/note/hierarchy', {
    method: 'PUT',
    data,
  });
}

// 批量更新笔记排序
export async function batchUpdateNoteOrder(data: { notes: { id: number; orderNum: number }[] }) {
  return request<BaseResponse<boolean>>('/api/note/order/batch', {
    method: 'PUT',
    data,
  });
}

// 获取报告的树形结构笔记
export async function getReportNoteTree(reportId: number) {
  return request<BaseResponse<LpNote[]>>(`/api/note/report/${reportId}/tree`, {
    method: 'GET',
  });
}

/** 批量更新笔记层级关系 */
export async function batchUpdateNoteHierarchy(params: { notes: { id: number; parentId: number; orderNum: number }[] }) {
  return request('/api/note/hierarchy/batch', {
    method: 'PUT',
    data: params,
  });
}

// 检索报告响应接口
export interface ListReportsRes {
  code: number;
  data: {
    size: number;
    records: {
      id: number;
      title: string;
      type: string;
      createTime: Record<string, unknown>;
      modifiedTime: Record<string, unknown>;
      userId: number;
    }[];
    total: number;
    current: number;
    pages: number;
  };
  message: string;
}

/** 
 * 分页查询检索报告列表
 * @param {number} current 页码
 * @param {number} size 每页大小
 * @param {string} keyword 搜索关键词
 * @returns
 */
export function listReports(current: number, size: number, keyword: string): Promise<ListReportsRes> {
  return request(`api/report/list`, {
    method: 'GET',
    params: {
      current,
      size,
      keyword
    }
  });
}

// 获取检索菜单报告列表
export async function getSearchSubjectReports() {
  return request<BaseResponse<LpSearchReport[]>>('/api/report/search-subjects', {
    method: 'GET',
  });
}
