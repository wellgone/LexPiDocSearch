export interface BookItem {
  id?: number;
  title: string;
  subTitle?: string;
  author?: string;
  publisher?: string;
  publicationYear?: string;
  publicationDate?: string;
  category?: string;
  keyWord?: string;
  summary?: string;
  cn?: string;
  series?: string;
  source?: string;
  note?: string;
  fileName?: string;
  fileId?: string;
  picUrl?: string;
  fileId?: number;
  pageSize?: number;
  type: number;
  status?: number;
  score?: number;
  isOcr?: number;
  isIndexed?: number;
  isOpaced?: number;
  createTime?: string;
  modifiedTime?: string;
}

export interface BookFormProps {
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onFinish: (values: BookItem) => Promise<boolean>;
  values?: BookItem;
  onSuccess?: () => void;
} 