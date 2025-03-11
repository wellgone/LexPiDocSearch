export interface Params {
  pageSize: number;
  pageNum: number;
  publicationYear?: number;
  source?: string;
  orderBy?: string;
  orderType?: 'asc' | 'desc';
  category?: string;
  author?: string;
  publisher?: string;
  keyword?: string;
  title?: string;
  yearFrom?: number;
  yearTo?: number;
  type?: number;
}

export interface ListItemDataType {
  id: number;
  title: string;
  likes: number;
  stars: number;
  viewCount: number;
  content?: string;
  picUrl?: string;
  fileName?: string;
  author?: string;
  publisher?: string;
  publicationYear?: string;
  isbn?: string;
  source?: string;
  score?: number;
  type: number;
  tags?: Array<{
    id: number;
    name: string;
  }>;
}

export interface SearchResponse<T = any> {
  code: number;
  message: string;
  data: {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
  };
}

export interface StandardFormRowProps {
  title?: string;
  last?: boolean;
  block?: boolean;
  grid?: boolean;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}

export interface TagSelectProps {
  expandable?: boolean;
  value?: string[] | number[];
  defaultValue?: string[] | number[];
  style?: React.CSSProperties;
  hideCheckAll?: boolean;
  actionsText?: {
    expandText?: string;
    collapseText?: string;
    selectAllText?: string;
  };
  onChange?: (value: string[] | number[]) => void;
  onExpand?: (expand: boolean) => void;
  multiple?: boolean;
  children?: React.ReactNode;
}

export interface TagSelectOptionProps {
  value: string | number;
  children?: React.ReactNode;
  style?: React.CSSProperties;
  checked?: boolean;
  onChange?: (value: string | number, state: boolean) => void;
}

export interface SearchHistory {
  id: string;
  keyword: string;
  timestamp: number;
  category?: string;
  filters?: {
    author?: string;
    publisher?: string;
    year?: number;
    source?: string;
  };
}

export interface Tag {
  id: string;
  name: string;
  parentId: string | null;
  level: number | null;
  createTime: string;
  modifiedTime: string;
  isDeleted: number;
  isIndexed: number;
}
