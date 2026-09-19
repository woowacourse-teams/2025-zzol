import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { useBlockedIps, useUnblockIp } from '@/api/queries';
import type { BlockedIp } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable } from '@/components/DataTable';
import { formatDurationMinutes } from '@/lib/format';

export function IpBlocksPage() {
  const [target, setTarget] = useState<BlockedIp | null>(null);
  const blocked = useBlockedIps();
  const unblock = useUnblockIp();

  const columns = useMemo<ColumnDef<BlockedIp, unknown>[]>(
    () => [
      {
        accessorKey: 'ip',
        header: 'IP',
        cell: (c) => <span className="font-mono">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'remainingTtlSeconds',
        header: '남은 차단 시간',
        meta: { align: 'right', width: '12rem' },
        cell: (c) => formatDurationMinutes(Math.round(Number(c.getValue()) / 60)),
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '8rem', align: 'right' },
        cell: (c) => (
          <Button variant="danger" size="sm" onClick={() => setTarget(c.row.original)}>
            차단 해제
          </Button>
        ),
      },
    ],
    [],
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="IP 차단"
        description="404 누적이나 악성 경로 접근으로 자동 차단된 IP입니다. 만료되면 목록에서 사라집니다."
      />

      <Card>
        {/* 건수는 성공했을 때만 적는다. 실패했는데 "0건"이라고 쓰면 차단된 IP 가
          * 없다는 말이 되어, 바로 아래 표의 오류 문구와 서로 다른 말을 한다. */}
        <CardHeader
          title="차단 중인 IP"
          description={blocked.data ? `${blocked.data.length}건` : undefined}
        />

        <DataTable
          error={blocked.error}
          onRetry={() => blocked.refetch()}
          columns={columns}
          data={blocked.data ?? []}
          loading={blocked.isPending}
          emptyTitle="차단 중인 IP가 없습니다"
          emptyDescription="자동 차단이 걸리면 여기에 나타납니다."
        />
      </Card>

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => !open && setTarget(null)}
        title="이 IP의 차단을 해제할까요?"
        description="해제하면 즉시 접속이 가능해집니다. 다시 차단되려면 차단 조건이 재발생해야 합니다."
        target={
          target
            ? `${target.ip} · 남은 차단 ${formatDurationMinutes(Math.round(target.remainingTtlSeconds / 60))}`
            : undefined
        }
        confirmLabel="차단 해제하기"
        pending={unblock.isPending}
        onConfirm={() => {
          if (!target) return;
          unblock.mutate(target.ip, { onSuccess: () => setTarget(null) });
        }}
      />
    </div>
  );
}
