import { ArrowLeft } from 'lucide-react';
import { useUserDetail } from '@/api/queries';
import { Button } from '@/components/ui/Button';
import { ContextPanel } from '@/components/ui/ContextPanel';
import { ErrorState, Skeleton } from '@/components/ui/EmptyState';
import { KeyValue } from '@/components/ui/KeyValue';
import { MetricRow } from '@/components/ui/MetricRow';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Timestamp } from '@/components/ui/Timestamp';
import { formatPercent } from '@/lib/format';
import { providerLabel } from '@/lib/labels';

/**
 * 유저 한 명.
 *
 * <p>지표를 타일 셋이 아니라 줄로 쌓았다. 참여와 당첨과 비율은 <b>서로 견줘야 하는
 * 값</b>이라 오른쪽 끝에 정렬해야 자릿수가 비교된다. 타일 셋을 늘어놓으면 서로 무관한
 * 지표로 읽히고, 좁은 패널에서는 한 칸이 120px 이라 값이 라벨보다 작아 보인다.
 *
 * <p>방에서 건너온 경우 돌아갈 링크를 준다. 조사는 방에서 사람으로 갔다가 다시 방으로
 * 오는 왕복이라, 되돌아갈 길이 없으면 목록에서 방을 다시 찾아야 한다.
 */
export function UserPanel({
  userId,
  onBack,
  onClose,
}: {
  userId: number | null;
  /** 방에서 건너온 경우에만 준다. 아니면 돌아갈 곳이 없으므로 링크도 없다. */
  onBack: (() => void) | null;
  onClose: () => void;
}) {
  const detail = useUserDetail(userId);
  const data = userId === null ? undefined : detail.data;

  return (
    <ContextPanel
      open={userId !== null}
      onClose={onClose}
      title={data ? (data.summary.nickname ?? data.summary.userCode) : '유저'}
      description={data ? `유저 #${data.summary.id}` : undefined}
    >
      {userId !== null && detail.isError && (
        <ErrorState message={(detail.error as Error).message} onRetry={() => detail.refetch()} />
      )}

      {userId !== null && detail.isPending && (
        <div className="flex flex-col gap-3">
          <Skeleton className="h-20" />
          <Skeleton className="h-24" />
        </div>
      )}

      {data && (
        <div className="flex flex-col gap-5">
          {onBack && (
            <Button variant="ghost" size="sm" className="-ml-2 self-start" onClick={onBack}>
              <ArrowLeft />
              방으로 돌아가기
            </Button>
          )}

          <dl className="flex flex-col">
            <MetricRow label="참여한 방" value={data.roomCount} />
            <MetricRow label="당첨" value={data.winCount} />
            <MetricRow
              label="당첨 비율"
              value={formatPercent(data.winRate)}
              hint="참여 대비. 기대값은 방마다 다름"
            />
          </dl>

          <KeyValue
            items={[
              {
                label: '유저코드',
                value: <span className="font-mono text-xs">{data.summary.userCode}</span>,
              },
              { label: '닉네임', value: data.summary.nickname },
              { label: '가입', value: <Timestamp value={data.summary.createdAt} /> },
              {
                label: '소셜 제공자',
                value:
                  data.providers.length === 0 ? null : (
                    <span className="flex flex-wrap gap-1.5">
                      {data.providers.map((provider) => (
                        <StatusBadge key={provider} tone="neutral">
                          {providerLabel(provider)}
                        </StatusBadge>
                      ))}
                    </span>
                  ),
              },
            ]}
          />
        </div>
      )}
    </ContextPanel>
  );
}
