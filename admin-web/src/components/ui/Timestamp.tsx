import { formatAbsolute, formatRelative } from '@/lib/format';
import { cn } from '@/lib/cn';

type TimestampProps = {
  value: string | Date | null | undefined;
  /** 상대시각을 숨긴다. 열 폭이 좁은 표에서 쓴다. */
  absoluteOnly?: boolean;
  /**
   * 절대시각을 숨기고 "3시간 전"만 남긴다.
   *
   * <p>요약 화면에서 쓴다. 거기서 묻는 것은 "언제였나"가 아니라 <b>"얼마나 됐나"</b>
   * 하나이고, 정확한 시각이 필요해지는 순간에는 어차피 그 종류의 화면으로 간다.
   * 툴팁에 절대시각이 남아 있어 잃는 정보는 없다.
   */
  relativeOnly?: boolean;
  className?: string;
};

/**
 * 절대시각과 상대시각을 함께 보여준다.
 *
 * 둘 다 필요하다. 상대시각만 있으면 "3일 전"이 정확히 언제인지 계산해야 하고,
 * 절대시각만 있으면 지금 급한 건인지 바로 안 보인다. 운영은 두 질문을 동시에 한다.
 */
export function Timestamp({ value, absoluteOnly, relativeOnly, className }: TimestampProps) {
  if (!value) {
    return <span className={cn('text-ink-muted', className)}>-</span>;
  }

  const absolute = formatAbsolute(value);

  if (relativeOnly) {
    return (
      <time
        dateTime={new Date(value).toISOString()}
        title={absolute}
        className={cn('whitespace-nowrap text-xs text-ink-secondary', className)}
      >
        {formatRelative(value)}
      </time>
    );
  }

  return (
    <span className={cn('whitespace-nowrap font-mono text-xs', className)}>
      <time dateTime={new Date(value).toISOString()}>{absolute}</time>
      {!absoluteOnly && <span className="ml-1.5 text-ink-muted">({formatRelative(value)})</span>}
    </span>
  );
}
