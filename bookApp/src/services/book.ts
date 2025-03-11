import request from '@/utils/request';

export interface BookItem {
  id?: number;
  title: string;
  subTitle?: string;
  author?: string;
  isbn: string;
  publisher?: string;
  publicationYear?: string;
  publicationDate?: string;
  category?: string;
  type?: number;
  status?: number;
  pageSize?: number;
  score?: number;
  keyWord?: string;
  summary?: string;
  cn?: string;
  series?: string;
  source?: string;
  note?: string;
  filePath?: string;
  picUrl?: string;
}

// 获取文件预览地址
export async function getPreviewUrl(id: number) {
  return request<{ code: number; data: string; message: string }>(`/api/book/preview/${id}`, {
    method: 'GET',
  });
}
// 通过文件名获取文件预览地址
export async function getPreviewUrlByFileName(fileName: string) {
  return request<{ code: number; data: string; message: string }>(`/api/book/preview/fileName/${fileName}`, {
    method: 'GET',
  });
}

// 批量提取和导入
export async function batchExtractAndImport() {
  return request<{ code: number; data: string; message: string }>('/api/book/batch/extract-import', {
    method: 'POST',
  });
}

// 查询批量提取和导入任务状态
export async function getBatchExtractImportStatus(taskId: string) {
  return request<{ code: number; data: any; message: string }>(
    `/api/book/batch/extract-import/status/${taskId}`,
    {
      method: 'GET',
    },
  );
} 