const MIN_WIDTH = 320;
const MAX_WIDTH = 430;
const MIN_HEIGHT = 568;
const MAX_HEIGHT = 932;

const createResponsiveClamp = (minViewport: number, maxViewport: number, unit: 'vw' | 'vh') => {
  return (min: number, max: number): string => {
    const slope = (max - min) / (maxViewport - minViewport);
    const unitValue = slope * 100;
    const offset = min - slope * minViewport;

    const offsetStr = offset >= 0 ? `+ ${offset}px` : `- ${Math.abs(offset)}px`;
    return `clamp(${min}px, ${unitValue}${unit} ${offsetStr}, ${max}px)`;
  };
};

export const responsiveWidth = createResponsiveClamp(MIN_WIDTH, MAX_WIDTH, 'vw');
export const responsiveHeight = createResponsiveClamp(MIN_HEIGHT, MAX_HEIGHT, 'vh');
