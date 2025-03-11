import request from '@/utils/request';

export interface CreateNoteParams {
  note: {
    content: string;
    sourceName?: string;
    sourcePress?: string;
    sourcePublicationDate?: string;
    sourceAuthor?: string;
    sourcePageSize?: number;
    createTime: string;
    modifiedTime: string;
    userId: string;
  };
  reportId?: number | null;
}

export interface CreateNoteRes {
  code: number;
  data: boolean;
  message: string;
}

export async function createNote(params: CreateNoteParams) {
  return request<API.Response<boolean>>('/api/note/create', {
    method: 'POST',
    data: params,
  });
}

export interface UpdateNoteParams {
  id: number;
  content: string;
  sourceName?: string;
  sourcePress?: string;
  sourceAuthor?: string;
  sourcePublicationDate?: string;
  sourcePageSize?: number;
  modifiedTime?: string;
  userId?: string | undefined;
  parentId?: number;
  orderNum?: number;
}

export async function updateNote(params: UpdateNoteParams) {
  return request<API.Response<boolean>>('/api/note/update', {
    method: 'PUT',
    data: params,
  });
}