import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { useNicknameSamples, useSampleDecision } from '@/api/queries';
import type { NicknameAudit } from '@/api/types';
import { DataTable } from '@/components/DataTable';
import { Button } from '@/components/ui/Button';
import { Card, CardHeader } from '@/components/ui/Card';
import { Pagination } from '@/components/ui/Pagination';
import { Timestamp } from '@/components/ui/Timestamp';

/**
 * 표본 검토.
 *
 * <p>AI가 CLEAN으로 통과시킨 닉네임은 다시 검열받지 않아 놓친 욕이 있어도 아무도 모른다.
 * 회차마다 일부를 뽑아 여기 올리고, 운영자가 고른 결과로 미탐률을 추정한다. 미탐은 차단과
 * 같은 경로를 타 사전에 오른다.
 *
 * <p>검열 대기와 달리 결정을 되돌릴 버튼이 없다. 결정하면 목록에서 빠지기 때문이다.
 * 그래도 확인 창을 두지 않는다. 대부분 정상이라 한 번에 스무 건을 누르는 화면이다.
 */
export function SampleReviewCard() {
  const [page, setPage] = useState(0);
  const samples = useNicknameSamples(page);
  const decide = useSampleDecision();

  const columns = useMemo<ColumnDef<NicknameAudit, unknown>[]>(
    () => [
      {
        accessorKey: 'nickname',
        header: '닉네임',
        meta: { width: '12rem' },
        cell: (c) => <span className="font-medium">{String(c.getValue())}</span>,
      },
      { accessorKey: 'reason', header: 'AI 사유' },
      {
        accessorKey: 'auditedAt',
        header: '통과',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} />,
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '10rem', align: 'right' },
        cell: (c) => (
          <span className="inline-flex gap-1.5">
            <Button
              size="sm"
              disabled={decide.isPending}
              onClick={() => decide.mutate({ id: c.row.original.id, decision: 'ok' })}
            >
              정상
            </Button>
            <Button
              size="sm"
              variant="danger"
              disabled={decide.isPending}
              onClick={() => decide.mutate({ id: c.row.original.id, decision: 'miss' })}
            >
              미탐
            </Button>
          </span>
        ),
      },
    ],
    [decide],
  );

  return (
    <Card>
      <CardHeader
        title="표본 검토"
        description="AI가 통과시킨 닉네임 중 무작위로 뽑은 것입니다. 걸렀어야 하면 미탐을 누릅니다."
      />

      <DataTable
        error={samples.error}
        onRetry={() => samples.refetch()}
        columns={columns}
        data={samples.data?.content ?? []}
        loading={samples.isPending}
        emptyTitle="검토할 표본이 없습니다"
        emptyDescription="검열 회차가 돌면 통과한 닉네임 일부가 여기에 올라옵니다."
      />
      {samples.data && (
        <Pagination
          page={samples.data.page}
          totalPages={samples.data.totalPages}
          totalElements={samples.data.totalElements}
          onChange={setPage}
        />
      )}
    </Card>
  );
}
