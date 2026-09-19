import { Legend } from '@/components/ui/Legend';

/**
 * 추이 차트 범례.
 *
 * <p>차트와 <b>다른 파일</b>에 둔다. 같은 모듈에 있으면 이 범례를 정적으로 가져오는
 * 순간 recharts 까지 딸려 와서, 차트를 지연 로드해도 번들이 쪼개지지 않는다.
 *
 * <p>이 파일이 아는 것은 "추이 차트에 어떤 계열이 어떤 모양으로 그려지는가" 하나뿐이다.
 * 표식을 그리는 일은 {@link Legend} 가 맡는다.
 */
const ITEMS = [
  { label: '방 생성', color: 'var(--chart-1)', shape: 'bar' },
  { label: '완주', color: 'var(--gray-300)', shape: 'bar' },
  { label: '참여자', color: 'var(--chart-1)', shape: 'line' },
] as const;

export function TrendLegend() {
  return <Legend items={[...ITEMS]} />;
}
