const KST = 'Asia/Seoul';

const ABSOLUTE = new Intl.DateTimeFormat('ko-KR', {
  timeZone: KST,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const NUMBER = new Intl.NumberFormat('ko-KR');

/**
 * `2026-09-06 14:32` (KST 고정). 운영자가 서버 시간대를 헷갈릴 일이 없어야 한다.
 *
 * 로케일 문자열을 정규식으로 자르지 않고 부품으로 조립한다. 치환은 로케일이나 런타임이
 * 바뀌면 조용히 어긋나는데, 시각이 틀린 것은 표에서 눈치채기 어렵다.
 */
export function formatAbsolute(value: string | Date): string {
  const parts = new Map(
    ABSOLUTE.formatToParts(toDate(value)).map((part) => [part.type, part.value]),
  );
  const hour = parts.get('hour') === '24' ? '00' : parts.get('hour');
  return `${parts.get('year')}-${parts.get('month')}-${parts.get('day')} ${hour}:${parts.get('minute')}`;
}

/**
 * `12분 전`. 절대시각과 함께 쓴다.
 * 상대시각만 있으면 "3일 전"이 언제인지 계산해야 하고, 절대시각만 있으면 지금 급한지 모른다.
 */
export function formatRelative(value: string | Date, now: Date = new Date()): string {
  const diffMs = now.getTime() - toDate(value).getTime();
  const future = diffMs < 0;
  const abs = Math.abs(diffMs);

  const minutes = Math.floor(abs / 60_000);
  if (minutes < 1) return '방금';
  if (minutes < 60) return suffix(`${minutes}분`, future);

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return suffix(`${hours}시간`, future);

  const days = Math.floor(hours / 24);
  if (days < 30) return suffix(`${days}일`, future);

  const months = Math.floor(days / 30);
  if (months < 12) return suffix(`${months}개월`, future);

  return suffix(`${Math.floor(months / 12)}년`, future);
}

/** 분 단위를 `2시간 15분` 처럼 읽히게. SLA 표시에 쓴다. */
export function formatDurationMinutes(minutes: number): string {
  if (minutes <= 0) return '0분';
  if (minutes < 60) return `${minutes}분`;

  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  if (hours < 24) return rest === 0 ? `${hours}시간` : `${hours}시간 ${rest}분`;

  const days = Math.floor(hours / 24);
  const restHours = hours % 24;
  return restHours === 0 ? `${days}일` : `${days}일 ${restHours}시간`;
}

export function formatNumber(value: number): string {
  return NUMBER.format(value);
}

/** `12.3%`. 0.0 ~ 1.0 비율을 받는다. */
export function formatPercent(ratio: number, digits = 1): string {
  return `${(ratio * 100).toFixed(digits)}%`;
}

function suffix(text: string, future: boolean): string {
  return future ? `${text} 후` : `${text} 전`;
}

function toDate(value: string | Date): Date {
  return value instanceof Date ? value : new Date(value);
}
