/** 이 값을 넘으면 밝은 배경으로 본다. colorList 9색을 눈으로 확인해 잡았다. */
const LIGHT_LUMINANCE_THRESHOLD = 150;

/** 참가자 색 위에 올릴 글자를 검게 할지 희게 할지 고른다. BT.601 휘도를 쓴다. */
export const isLightColor = (hexColor: string): boolean => {
  const rgb = Number.parseInt(hexColor.replace('#', ''), 16);
  const red = (rgb >> 16) & 0xff;
  const green = (rgb >> 8) & 0xff;
  const blue = rgb & 0xff;

  return (red * 299 + green * 587 + blue * 114) / 1000 > LIGHT_LUMINANCE_THRESHOLD;
};
