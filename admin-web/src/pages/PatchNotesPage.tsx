import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDeletePatchNote, usePatchNotes } from '@/api/queries';
import type { PatchNote, PatchNoteCategory } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Timestamp } from '@/components/ui/Timestamp';
import { DataTable } from '@/components/DataTable';

const CATEGORY_LABEL: Record<PatchNoteCategory, string> = {
  NOTICE: '공지',
  EVENT: '이벤트',
  UPDATE: '업데이트',
  MAINTENANCE: '점검',
};

export function PatchNotesPage() {
  const notes = usePatchNotes();
  const remove = useDeletePatchNote();
  const [target, setTarget] = useState<PatchNote | null>(null);

  const columns = useMemo<ColumnDef<PatchNote, unknown>[]>(
    () => [
      {
        accessorKey: 'category',
        header: '분류',
        meta: { width: '7rem' },
        cell: (c) => (
          <StatusBadge tone="neutral">
            {CATEGORY_LABEL[c.getValue() as PatchNoteCategory]}
          </StatusBadge>
        ),
      },
      {
        accessorKey: 'title',
        header: '제목',
        cell: (c) => <span className="font-medium">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'createdAt',
        header: '작성',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} absoluteOnly />,
      },
      {
        accessorKey: 'updatedAt',
        header: '수정',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} absoluteOnly />,
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '9rem', align: 'right' },
        cell: (c) => (
          <div className="flex justify-end gap-1.5">
            <Button asChild variant="secondary" size="sm">
              <Link to={`/patch-notes/${c.row.original.id}`}>수정</Link>
            </Button>
            <Button variant="danger" size="sm" onClick={() => setTarget(c.row.original)}>
              삭제
            </Button>
          </div>
        ),
      },
    ],
    [],
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="패치노트"
        description="서비스 화면에 노출되는 공지입니다. 저장하면 즉시 반영됩니다."
      />

      <Card>
        <CardHeader
          title="패치노트 목록"
          description={notes.data ? `${notes.data.length}건` : undefined}
          actions={
            <Button asChild variant="primary" size="sm">
              <Link to="/patch-notes/new">새 글 작성</Link>
            </Button>
          }
        />

        <DataTable
          error={notes.error}
          onRetry={() => notes.refetch()}
          columns={columns}
          data={notes.data ?? []}
          loading={notes.isPending}
          emptyTitle="패치노트가 없습니다"
        />
      </Card>

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => !open && setTarget(null)}
        title="이 패치노트를 삭제할까요?"
        description="삭제하면 서비스 화면에서 즉시 사라집니다. 되돌릴 수 없습니다."
        target={target ? `${CATEGORY_LABEL[target.category]} · ${target.title}` : undefined}
        confirmLabel="삭제하기"
        pending={remove.isPending}
        onConfirm={() => {
          if (!target) return;
          remove.mutate(target.id, { onSuccess: () => setTarget(null) });
        }}
      />
    </div>
  );
}
