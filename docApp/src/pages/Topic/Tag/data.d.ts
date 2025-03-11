export interface TagItem {
  id?: number;
  name: string;
  parentId?: string | null;
  level?: number;
  type?: string;
  createTime?: Date;
  modifiedTime?: Date;
  isDeleted?: number;
  isIndexed?: number;

}

export interface TagFormData {
  name: string;
  level: number;
} 