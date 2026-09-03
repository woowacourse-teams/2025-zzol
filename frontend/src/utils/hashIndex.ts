/**
 * 이름을 길이 안의 인덱스 하나로 접는다.
 *
 * 같은 이름이면 언제나 같은 값이 나오므로 서버 목록이 아직 없어도 사람마다 다른 색·모양을 줄 수 있다.
 */
export const hashIndex = (value: string, length: number): number =>
  [...value].reduce((hash, char) => (hash * 31 + char.charCodeAt(0)) % length, 7 % length);
