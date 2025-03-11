export interface DocSection {
  id?: number;
  docId: number;
  pageNum: number;
  content: string;
  createTime?: Date;
  modifiedTime?: Date;
} 