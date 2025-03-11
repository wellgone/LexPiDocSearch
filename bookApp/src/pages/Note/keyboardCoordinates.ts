import type {SensorContext} from './types';
import type {KeyboardCoordinateGetter} from '@dnd-kit/core';

const baseCoordinates = {x: 0, y: 0};

export const sortableTreeKeyboardCoordinates: (
  context: SensorContext,
  indicator: boolean,
  indentationWidth: number
) => KeyboardCoordinateGetter = (context, indicator, indentationWidth) => (
  event,
  {currentCoordinates}
) => {
  if (!event.code) {
    return baseCoordinates;
  }

  const {items, offset} = context.current;
  const activeId = event.active?.id;
  const overId = event.over?.id;

  if (!activeId || !overId) {
    return baseCoordinates;
  }

  const overItemIndex = items.findIndex(({id}) => id === overId);
  const activeItemIndex = items.findIndex(({id}) => id === activeId);

  const activeItem = items[activeItemIndex];

  switch (event.code) {
    case 'ArrowRight':
      if (!indicator) {
        return currentCoordinates;
      }

      event.preventDefault();
      {
        const previousItem = items[overItemIndex - 1];

        if (!previousItem) {
          return baseCoordinates;
        }

        return {
          ...currentCoordinates,
          x: offset + indentationWidth,
        };
      }
    case 'ArrowLeft':
      if (!indicator) {
        return currentCoordinates;
      }

      event.preventDefault();

      if (activeItem.depth === 0) {
        return baseCoordinates;
      }

      return {
        ...currentCoordinates,
        x: offset - indentationWidth,
      };
    default:
      return baseCoordinates;
  }
}; 