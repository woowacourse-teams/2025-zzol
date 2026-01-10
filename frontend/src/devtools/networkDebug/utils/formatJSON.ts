/**
 * JSON 문자열을 포맷팅합니다.
 * 유효한 JSON이면 들여쓰기된 형태로 반환하고, 그렇지 않으면 원본을 반환합니다.
 *
 * @param text - 포맷팅할 텍스트
 * @returns 포맷팅된 JSON 문자열 또는 원본 텍스트
 */
export const formatJSON = (text: string): string => {
  try {
    const parsed = JSON.parse(text);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return text;
  }
};
