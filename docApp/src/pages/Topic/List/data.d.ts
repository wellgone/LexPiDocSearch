export interface TopicItem {
  id?: string;
  name: string;
  parentId?: string;
  level?: number;
  isIndexed?: number;
  createTime?: string;
  modifiedTime?: string;
  isDeleted?: number;
  children?: TopicItem[];
} 