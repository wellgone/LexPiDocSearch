import React, {useEffect, useMemo, useRef, useState} from 'react';
import {createPortal} from 'react-dom';
import {
  Announcements,
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragStartEvent,
  DragOverlay,
  DragMoveEvent,
  DragEndEvent,
  DragOverEvent,
  MeasuringStrategy,
  DropAnimation,
  Modifier,
  defaultDropAnimation,
  UniqueIdentifier,
} from '@dnd-kit/core';
import {
  SortableContext,
  arrayMove,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';

import {
  buildTree,
  flattenTree,
  getProjection,
  getChildCount,
  removeItem,
  removeChildrenOf,
  setProperty,
} from '../utilities';
import type {FlattenedItem, SensorContext, TreeItems} from '../types';
import {sortableTreeKeyboardCoordinates} from '../keyboardCoordinates';
import {SortableTreeItem} from './index';
import {CSS} from '@dnd-kit/utilities';

const measuring = {
  droppable: {
    strategy: MeasuringStrategy.Always,
  },
};

const dropAnimationConfig: DropAnimation = {
  keyframes({transform}) {
    return [
      {opacity: 1, transform: CSS.Transform.toString(transform.initial)},
      {
        opacity: 0,
        transform: CSS.Transform.toString({
          ...transform.final,
          x: transform.final.x + 5,
          y: transform.final.y + 5,
        }),
      },
    ];
  },
  easing: 'ease-out',
  sideEffects({active}) {
    active.node.animate([{opacity: 0}, {opacity: 1}], {
      duration: defaultDropAnimation.duration,
      easing: defaultDropAnimation.easing,
    });
  },
};

interface Props {
  collapsible?: boolean;
  defaultItems?: TreeItems;
  indentationWidth?: number;
  indicator?: boolean;
  removable?: boolean;
  items: TreeItems;
  onItemsChange: (items: TreeItems, isCollapsedChange?: boolean) => void;
  onEditNote?: (id: string, value: string) => void;
  onRemoveNote?: (id: string) => void;
  groupIndex?: number;
}

export function SortableTree({
  collapsible,
  indicator = false,
  indentationWidth = 50,
  removable,
  items,
  onItemsChange,
  onEditNote,
  onRemoveNote,
  groupIndex = 0,
}: Props) {
  const [activeId, setActiveId] = useState<UniqueIdentifier | null>(null);
  const [overId, setOverId] = useState<UniqueIdentifier | null>(null);
  const [offsetLeft, setOffsetLeft] = useState(0);
  const [currentPosition, setCurrentPosition] = useState<{
    parentId: UniqueIdentifier | null;
    overId: UniqueIdentifier;
  } | null>(null);

  const flattenedItems = useMemo(() => {
    const flattenedTree = flattenTree(items);
    const collapsedItems = flattenedTree.reduce<string[]>(
      (acc, {children, collapsed, id}) =>
        collapsed && children.length ? [...acc, id] : acc,
      []
    );

    return removeChildrenOf(
      flattenedTree,
      activeId !== null ? [activeId, ...collapsedItems] : collapsedItems
    );
  }, [activeId, items]);

  const projected =
    activeId && overId
      ? getProjection(
          flattenedItems,
          activeId,
          overId,
          offsetLeft,
          indentationWidth
        )
      : null;

  const sensorContext: SensorContext = useRef({
    items: flattenedItems,
    offset: offsetLeft,
  });

  const [coordinateGetter] = useState(() =>
    sortableTreeKeyboardCoordinates(sensorContext, indicator, indentationWidth)
  );

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter,
    })
  );

  const sortedIds = useMemo(
    () => flattenedItems.map(({id}) => id),
    [flattenedItems]
  );

  const activeItem = activeId
    ? flattenedItems.find(({id}) => id === activeId)
    : null;

  useEffect(() => {
    sensorContext.current = {
      items: flattenedItems,
      offset: offsetLeft,
    };
  }, [flattenedItems, offsetLeft]);

  const announcements: Announcements = {
    onDragStart({active}) {
      return `已选中 ${active.id}。`;
    },
    onDragMove({active, over}) {
      return getMovementAnnouncement('onDragMove', active.id, over?.id);
    },
    onDragOver({active, over}) {
      return getMovementAnnouncement('onDragOver', active.id, over?.id);
    },
    onDragEnd({active, over}) {
      return getMovementAnnouncement('onDragEnd', active.id, over?.id);
    },
    onDragCancel({active}) {
      return `移动已取消。${active.id} 已返回原位置。`;
    },
  };

  return (
    <DndContext
      accessibility={{announcements}}
      sensors={sensors}
      collisionDetection={closestCenter}
      measuring={measuring}
      onDragStart={handleDragStart}
      onDragMove={handleDragMove}
      onDragOver={handleDragOver}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <SortableContext items={sortedIds} strategy={verticalListSortingStrategy}>
        {flattenedItems.map(({id, children, collapsed, depth, content, pageSize, sourceName, sourcePress, sourceAuthor, sourcePublicationDate, parentId}, index) => {
          let itemNumber = '';
          
          const getSiblingIndex = (itemId: UniqueIdentifier, parentId: UniqueIdentifier | null) => {
            const siblings = flattenedItems.filter(item => item.parentId === parentId);
            return siblings.findIndex(item => item.id === itemId);
          };

          const getParentNumber = (itemId: UniqueIdentifier): string => {
            const item = flattenedItems.find(i => i.id === itemId);
            if (!item || !item.parentId) return '';
            
            const parent = flattenedItems.find(i => i.id === item.parentId);
            if (!parent) return '';

            const parentSiblingIndex = getSiblingIndex(parent.id, parent.parentId);
            
            if (!parent.parentId) {
              return `${groupIndex + 1}.${parentSiblingIndex + 1}`;
            }
            
            const grandParentNumber = getParentNumber(parent.id);
            return `${grandParentNumber}.${parentSiblingIndex + 1}`;
          };

          if (!parentId) {
            const siblingIndex = getSiblingIndex(id, null);
            itemNumber = `${groupIndex + 1}.${siblingIndex + 1}`;
          } else {
            const parentNumber = getParentNumber(id);
            const siblingIndex = getSiblingIndex(id, parentId);
            itemNumber = parentNumber ? `${parentNumber}.${siblingIndex + 1}` : '';
          }

          return (
            <SortableTreeItem
              key={id}
              id={String(id)}
              value={content}
              depth={depth}
              pageSize={pageSize}
              sourceInfo={{
                sourceName: sourceName || undefined,
                sourcePress: sourcePress || undefined,
                sourceAuthor: sourceAuthor || undefined,
                sourcePublicationDate: sourcePublicationDate || undefined,
              }}
              indicator={indicator}
              indentationWidth={indentationWidth}
              collapsed={Boolean(collapsed && children?.length)}
              onCollapse={
                collapsible && children?.length
                  ? () => handleCollapse(id)
                  : undefined
              }
              onRemove={removable ? () => onRemoveNote?.(id.toString()) : undefined}
              onEdit={onEditNote}
              groupIndex={groupIndex}
              itemIndex={index}
              parentNumber={itemNumber}
            />
          );
        })}
        {createPortal(
          <DragOverlay
            dropAnimation={dropAnimationConfig}
            modifiers={indicator ? [adjustTranslate] : undefined}
          >
            {activeId && activeItem ? (
              <SortableTreeItem
                id={activeId}
                depth={activeItem.depth}
                clone
                childCount={getChildCount(items, activeId) + 1}
                value={activeItem.content}
                indentationWidth={indentationWidth}
              />
            ) : null}
          </DragOverlay>,
          document.body
        )}
      </SortableContext>
    </DndContext>
  );

  function handleDragStart({active: {id: activeId}}: DragStartEvent) {
    setActiveId(activeId);
    setOverId(activeId);

    const activeItem = flattenedItems.find(({id}) => id === activeId);

    if (activeItem) {
      setCurrentPosition({
        parentId: activeItem.parentId,
        overId: activeId,
      });
    }

    document.body.style.setProperty('cursor', 'grabbing');
  }

  function handleDragMove({delta}: DragMoveEvent) {
    setOffsetLeft(delta.x);
  }

  function handleDragOver({over}: DragOverEvent) {
    setOverId(over?.id ?? null);
  }

  function handleDragEnd({active, over}: DragEndEvent) {
    resetState();

    if (projected && over) {
      const {depth, parentId} = projected;
      const clonedItems: FlattenedItem[] = JSON.parse(
        JSON.stringify(flattenTree(items))
      );
      const overIndex = clonedItems.findIndex(({id}) => id === over.id);
      const activeIndex = clonedItems.findIndex(({id}) => id === active.id);
      const activeTreeItem = clonedItems[activeIndex];

      const isSamePosition = 
        activeIndex === overIndex && 
        activeTreeItem.depth === depth && 
        activeTreeItem.parentId === parentId;

      if (isSamePosition) {
        return;
      }

      clonedItems[activeIndex] = {...activeTreeItem, depth, parentId};

      const sortedItems = arrayMove(clonedItems, activeIndex, overIndex);
      const newItems = buildTree(sortedItems);

      onItemsChange(newItems);
    }
  }

  function handleDragCancel() {
    resetState();
  }

  function resetState() {
    setOverId(null);
    setActiveId(null);
    setOffsetLeft(0);
    setCurrentPosition(null);

    document.body.style.setProperty('cursor', '');
  }

  function handleCollapse(id: UniqueIdentifier) {
    const newItems = setProperty(items, id, 'collapsed', (value) => !value);
    onItemsChange(newItems, true);
  }

  function getMovementAnnouncement(
    eventName: string,
    activeId: UniqueIdentifier,
    overId?: UniqueIdentifier
  ) {
    if (overId && projected) {
      if (eventName !== 'onDragEnd') {
        if (
          currentPosition &&
          projected.parentId === currentPosition.parentId &&
          overId === currentPosition.overId
        ) {
          return;
        } else {
          setCurrentPosition({
            parentId: projected.parentId,
            overId,
          });
        }
      }

      const clonedItems: FlattenedItem[] = JSON.parse(
        JSON.stringify(flattenTree(items))
      );
      const overIndex = clonedItems.findIndex(({id}) => id === overId);
      const activeIndex = clonedItems.findIndex(({id}) => id === activeId);
      const sortedItems = arrayMove(clonedItems, activeIndex, overIndex);

      const previousItem = sortedItems[overIndex - 1];

      let announcement;
      const movedVerb = eventName === 'onDragEnd' ? '已放置到' : '已移动到';
      const nestedVerb = eventName === 'onDragEnd' ? '已放置到' : '已嵌套到';

      if (!previousItem) {
        const nextItem = sortedItems[overIndex + 1];
        announcement = nextItem ? `${activeId} ${movedVerb} ${nextItem.id} 之前。` : `${activeId} ${movedVerb}列表末尾。`;
      } else {
        if (projected.depth > previousItem.depth) {
          announcement = `${activeId} ${nestedVerb} ${previousItem.id} 下。`;
        } else {
          let previousSibling: FlattenedItem | undefined = previousItem;
          while (previousSibling && projected.depth < previousSibling.depth) {
            const parentId: UniqueIdentifier | null = previousSibling.parentId;
            previousSibling = sortedItems.find(({id}) => id === parentId);
          }

          if (previousSibling) {
            announcement = `${activeId} ${movedVerb} ${previousSibling.id} 之后。`;
          }
        }
      }

      return announcement;
    }

    return;
  }
}

const adjustTranslate: Modifier = ({transform}) => {
  return {
    ...transform,
    y: transform.y - 25,
  };
};