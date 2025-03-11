export interface DocItem {
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

export interface DocFormProps {
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onFinish: (values: DocItem) => Promise<boolean>;
  values?: DocItem;
  onSuccess?: () => void;
} 