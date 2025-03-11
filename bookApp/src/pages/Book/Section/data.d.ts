export interface BookSection {
  id?: number;
  bookId: number;
  pageNum: number;
  content: string;
  createTime?: Date;
  modifiedTime?: Date;
} 