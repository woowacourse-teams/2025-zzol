const percentFormat = new Intl.NumberFormat('ko-KR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/**
 * 확률을 세로로 쌓아 보여주는 화면에서 쓰는 표기.
 *
 * 서버는 확률을 0~10000 정수(퍼센트의 1/100 단위)로 다루고 100 으로 나눠 보낸다.
 * 즉 값은 항상 `n/100` 이라 소수점 둘째 자리까지가 도메인의 실제 정밀도다.
 * 그대로 찍으면 JS 가 뒤 0 을 지워 `20%`·`12.5%`·`8.33%` 처럼 소수점 위치가 어긋나,
 * 세로로 쌓았을 때 자릿수를 눈으로 비교할 수 없다. 그래서 두 자리로 고정한다.
 *
 * 스크린리더 낭독에는 쓰지 않는다 — `20.00%` 는 "이십 점 영영 퍼센트"로 읽혀
 * 참가자 수만큼 반복되면 듣기 나쁘다. 낭독은 `describeProbabilities` 가 원본을 쓴다.
 */
export const formatProbability = (probability: number) => `${percentFormat.format(probability)}%`;

/** 확률 변화량. 늘었으면 부호를 붙여 방향이 드러나게 한다. */
export const formatProbabilityChange = (change: number) =>
  `${change >= 0 ? '+' : ''}${percentFormat.format(change)}%`;
