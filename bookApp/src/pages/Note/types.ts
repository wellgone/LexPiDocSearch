import type {MutableRefObject} from 'react';
import type {UniqueIdentifier} from '@dnd-kit/core';

export interface TreeItem {
  id: UniqueIdentifier;
  content: string;
  children: TreeItem[];
  collapsed?: boolean;
  pageSize?: number;
  sourceName?: string;
  sourcePress?: string;
  sourceAuthor?: string;
  sourcePublicationDate?: string;
}

export type TreeItems = TreeItem[];

export interface FlattenedItem extends TreeItem {
  parentId: UniqueIdentifier | null;
  depth: number;
  index?: number;
}

export type SensorContext = MutableRefObject<{
  items: FlattenedItem[];
  offset: number;
}>;
