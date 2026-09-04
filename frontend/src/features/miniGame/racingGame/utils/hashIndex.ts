/**
 * 이름을 길이 안의 인덱스 하나로 접는다.
 *
 * 같은 이름이면 언제나 같은 값이 나오므로 참가자 명단이 아직 없어도 사람마다 다른 모양을 줄 수 있다.
 * 색은 서버 colorIndex 가 원천이라 이걸 쓰지 않는다.
 */
export const hashIndex = (value: string, length: number): number =>
  [...value].reduce((hash, char) => (hash * 31 + char.charCodeAt(0)) % length, 7 % length);
