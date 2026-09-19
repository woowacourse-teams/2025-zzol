import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import {
  useAddAdminAccount,
  useAdminAccounts,
  useRemoveAdminAccount,
} from '@/api/queries';
import type { AdminAccount } from '@/api/types';
import { useAuth } from '@/auth/AuthProvider';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Input } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Timestamp } from '@/components/ui/Timestamp';
import { DataTable } from '@/components/DataTable';

export function AdminAccountsPage() {
  const auth = useAuth();
  const accounts = useAdminAccounts();
  const add = useAddAdminAccount();
  const remove = useRemoveAdminAccount();

  const [email, setEmail] = useState('');
  const [addError, setAddError] = useState<string | null>(null);
  const [target, setTarget] = useState<AdminAccount | null>(null);

  const me = auth.status === 'authenticated' ? auth.email : null;

  const columns = useMemo<ColumnDef<AdminAccount, unknown>[]>(
    () => [
      {
        accessorKey: 'email',
        header: '이메일',
        cell: (c) => (
          <span className="flex items-center gap-2">
            <span className="font-medium">{String(c.getValue())}</span>
            {c.getValue() === me && <StatusBadge tone="neutral">나</StatusBadge>}
          </span>
        ),
      },
      {
        accessorKey: 'source',
        header: '출처',
        meta: { width: '11rem' },
        cell: (c) =>
          c.getValue() === 'BOOTSTRAP' ? (
            <StatusBadge tone="neutral">환경변수</StatusBadge>
          ) : (
            <StatusBadge tone="muted">UI 추가</StatusBadge>
          ),
      },
      {
        accessorKey: 'createdByEmail',
        header: '추가한 사람',
        meta: { width: '14rem' },
        cell: (c) =>
          c.getValue() ? (
            <span className="text-ink-secondary">{String(c.getValue())}</span>
          ) : (
            <span className="text-ink-muted">-</span>
          ),
      },
      {
        accessorKey: 'createdAt',
        header: '추가',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string | null} absoluteOnly />,
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '7rem', align: 'right' },
        cell: (c) => {
          const account = c.row.original;
          // 환경변수 관리자는 서버가 삭제를 막는다. 누를 수 없는 버튼을 보여주지 않는다.
          // 자기 자신도 마찬가지다. 지우면 그 순간 로그아웃된다.
          if (!account.removable || account.email === me) {
            return null;
          }
          return (
            <Button variant="danger" size="sm" onClick={() => setTarget(account)}>
              삭제
            </Button>
          );
        },
      },
    ],
    [me],
  );

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setAddError(null);
    add.mutate(email, {
      onSuccess: () => setEmail(''),
      onError: (error) => setAddError((error as Error).message),
    });
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="관리자"
        description="추가하면 즉시 로그인할 수 있습니다. 승인 대기 단계는 없습니다. 모든 조치는 감사 로그에 실행자와 함께 남습니다."
      />

      {/* 목록과 추가 폼을 나란히 둔다.
        *
        * 세로로 쌓았더니 추가 폼 카드가 본문 폭 1320px 를 다 쓰면서 입력 하나와 버튼
        * 하나만 놓였고 오른쪽 760px 가 비었다. 목록도 열이 다섯뿐이라 같은 문제를 겪었다.
        * 나란히 놓으면 둘 다 제 폭을 찾고 화면 세로도 절반이 된다. */}
      <div className="grid items-start gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(17rem,1fr)]">
        <Card>
          <CardHeader
            title="관리자 목록"
            description="환경변수(ADMIN_EMAILS)로 등록된 관리자는 UI에서 삭제할 수 없습니다. 전원이 잠기는 것을 막는 경로입니다."
          />
          <DataTable
            error={accounts.error}
            onRetry={() => accounts.refetch()}
            columns={columns}
            data={accounts.data ?? []}
            loading={accounts.isPending}
            emptyTitle="관리자가 없습니다"
          />
        </Card>

        <Card>
          <CardHeader title="관리자 추가" description="승인 대기 단계는 없습니다." />
          <CardBody>
            {/* 좁은 열이라 세로로 쌓는다. 가로로 두면 입력이 140px 까지 줄어 이메일이
              * 통째로 잘린다. */}
            <form onSubmit={submit} className="flex flex-col gap-2">
              <Input
                type="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="admin@zzol.site"
              />
              {addError && <p className="text-xs text-attention">{addError}</p>}
              <Button type="submit" variant="primary" disabled={add.isPending}>
                추가하기
              </Button>
            </form>
          </CardBody>
        </Card>
      </div>

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => !open && setTarget(null)}
        title="이 관리자를 삭제할까요?"
        description="삭제하면 즉시 백오피스에 들어올 수 없습니다. 다시 추가하면 복구됩니다."
        target={target?.email}
        confirmLabel="삭제하기"
        pending={remove.isPending}
        onConfirm={() => {
          if (!target?.id) return;
          remove.mutate(target.id, { onSuccess: () => setTarget(null) });
        }}
      />
    </div>
  );
}
