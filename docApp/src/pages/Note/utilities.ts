import type {UniqueIdentifier} from '@dnd-kit/core';
import {arrayMove} from '@dnd-kit/sortable';

import type {FlattenedItem, TreeItem, TreeItems} from './types';

export const iOS = /iPad|iPhone|iPod/.test(navigator.platform);

function getDragDepth(offset: number, indentationWidth: number) {
  return Math.round(offset / indentationWidth);
}

export function getProjection(
  items: FlattenedItem[],
  activeId: UniqueIdentifier,
  overId: UniqueIdentifier,
  dragOffset: number,
  indentationWidth: number
) {
  const overItemIndex = items.findIndex(({id}) => id === overId);
  const activeItemIndex = items.findIndex(({id}) => id === activeId);
  const activeItem = items[activeItemIndex];
  const newItems = arrayMove(items, activeItemIndex, overItemIndex);
  const previousItem = newItems[overItemIndex - 1];
  const nextItem = newItems[overItemIndex + 1];
  const dragDepth = getDragDepth(dragOffset, indentationWidth);
  const projectedDepth = activeItem.depth + dragDepth;
  const maxDepth = getMaxDepth({
    previousItem,
  });
  const minDepth = getMinDepth({nextItem});
  let depth = projectedDepth;

  if (projectedDepth >= maxDepth) {
    depth = maxDepth;
  } else if (projectedDepth < minDepth) {
    depth = minDepth;
  }

  return {depth, maxDepth, minDepth, parentId: getParentId()};

  function getParentId() {
    if (depth === 0 || !previousItem) {
      return null;
    }

    if (depth === previousItem.depth) {
      return previousItem.parentId;
    }

    if (depth > previousItem.depth) {
      return previousItem.id;
    }

    const newParent = newItems
      .slice(0, overItemIndex)
      .reverse()
      .find((item) => item.depth === depth)?.parentId;

    return newParent ?? null;
  }
}

function getMaxDepth({previousItem}: {previousItem: FlattenedItem}) {
  if (previousItem) {
    return previousItem.depth + 1;
  }

  return 0;
}

function getMinDepth({nextItem}: {nextItem: FlattenedItem}) {
  if (nextItem) {
    return nextItem.depth;
  }

  return 0;
}

export function flattenTree(items: TreeItems): FlattenedItem[] {
  const flattenedTree: FlattenedItem[] = [];

  function flatten(items: TreeItems, parentId: UniqueIdentifier | null, depth = 0) {
    for (const item of items) {
      const {id, children = [], ...rest} = item;

      flattenedTree.push({
        ...rest,
        id,
        children: children || [],
        parentId,
        depth,
      });

      if (children && children.length > 0) {
        flatten(children, id, depth + 1);
      }
    }
  }

  flatten(items, null);

  return flattenedTree;
}

function flatten(
  items: TreeItems,
  parentId: UniqueIdentifier | null = null,
  depth = 0
): FlattenedItem[] {
  return items.reduce<FlattenedItem[]>((acc, item, index) => {
    const children = item.children || [];
    return [
      ...acc,
      {...item, parentId, depth, index},
      ...flatten(children, item.id, depth + 1),
    ];
  }, []);
}

export function buildTree(flattenedItems: FlattenedItem[]): TreeItems {
  const root: TreeItem = {id: 'root', content: '', children: []};
  const nodes: Record<string, TreeItem> = {[root.id]: root};
  const items = flattenedItems.map((item) => ({...item, children: []}));

  for (const item of items) {
    const {id, children} = item;
    const parentId = item.parentId ?? root.id;

    const parent = nodes[parentId] ?? findItem(items, parentId);

    nodes[id] = {
      ...item,
      children,
    };

    if (parent) {
      parent.children = parent.children ?? [];
      parent.children.push(nodes[id]);
    }
  }

  return root.children;
}

export function findItem(items: TreeItems, itemId: UniqueIdentifier): TreeItem | null {
  return items.find(({id}) => id === itemId) || null;
}

export function findItemDeep(
  items: TreeItems,
  itemId: UniqueIdentifier
): TreeItem | null {
  if (!items) return null;

  for (const item of items) {
    if (!item) continue;
    
    const {id, children = []} = item;

    if (id === itemId) {
      return item;
    }

    if (children && children.length > 0) {
      const child = findItemDeep(children, itemId);

      if (child) {
        return child;
      }
    }
  }

  return null;
}

export function removeItem(items: TreeItems, id: UniqueIdentifier) {
  const newItems = [];

  for (const item of items) {
    if (item.id === id) {
      continue;
    }

    if (item.children.length) {
      item.children = removeItem(item.children, id);
    }

    newItems.push(item);
  }

  return newItems;
}

export function setProperty<T extends keyof TreeItem>(
  items: TreeItems,
  id: UniqueIdentifier,
  property: T,
  setter: (value: TreeItem[T]) => TreeItem[T]
) {
  if (!items) return items;

  for (const item of items) {
    if (!item) continue;

    if (item.id === id) {
      item[property] = setter(item[property]);
      continue;
    }

    if (item.children && item.children.length) {
      item.children = setProperty(item.children, id, property, setter);
    }
  }

  return [...items];
}

function countChildren(items: TreeItem[], count = 0): number {
  return items.reduce((acc, {children}) => {
    if (children.length) {
      return countChildren(children, acc + 1);
    }

    return acc + 1;
  }, count);
}

export function getChildCount(items: TreeItems, id: UniqueIdentifier) {
  if (!items || !id) return 0;
  
  const item = findItemDeep(items, id);

  if (!item || !item.children) {
    return 0;
  }

  const stack = [...item.children];
  let count = 0;

  while (stack.length > 0) {
    const currentItem = stack.pop();
    if (!currentItem) continue;

    count++;
    if (currentItem.children && currentItem.children.length > 0) {
      stack.push(...currentItem.children);
    }
  }

  return count;
}

export function removeChildrenOf(
  items: FlattenedItem[],
  ids: UniqueIdentifier[]
): FlattenedItem[] {
  const excludeParentIds = [...ids];

  return items.filter((item) => {
    if (item.parentId && excludeParentIds.includes(item.parentId)) {
      if (item.children.length) {
        excludeParentIds.push(item.id);
      }
      return false;
    }

    return true;
  });
}
