import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { useAuditLogStats, useAuditLogs } from '@/api/queries';
import type { AdminAuditLog, AdminAuditResult } from '@/api/types';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { SearchInput, Select } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Timestamp } from '@/components/ui/Timestamp';
import { DataTable } from '@/components/DataTable';
import { DailyChart } from '@/components/charts/DailyChart';
import { DistributionBars } from '@/components/charts/DistributionBars';
import { Skeleton } from '@/components/ui/EmptyState';
import { Loaded } from '@/components/ui/Loaded';
import { ShareNote } from '@/components/ui/ShareNote';
import { auditActionLabel } from '@/lib/labels';
import { useDebounced } from '@/lib/useDebounced';

/** 기간 선택지. 비움은 "전 기간"이다. */
const RANGES = [
  { value: '', label: '전 기간' },
  { value: '1', label: '오늘' },
  { value: '7', label: '7일' },
  { value: '30', label: '30일' },
];

/** 실패가 코랄이다. 되돌려야 할 일이 있다는 신호라 그 막대가 늘어나는 것을 먼저 봐야 한다. */
const DAILY_SERIES = [
  { key: 'failure', name: '실패', color: 'var(--chart-1)' },
  { key: 'success', name: '성공', color: 'var(--gray-300)' },
];

export function AuditLogsPage() {
  const [page, setPage] = useState(0);
  const [actorInput, setActorInput] = useState('');
  const [result, setResult] = useState<AdminAuditResult | ''>('');
  const [days, setDays] = useState('');

  const stats = useAuditLogStats(30);

  // 타이핑마다 서버를 때리지 않는다. 이메일 앞자리를 치는 동안 예닐곱 번 조회된다.
  const actorEmail = useDebounced(actorInput, 300);

  const logs = useAuditLogs({
    page,
    size: 30,
    actorEmail: actorEmail || undefined,
    result: result || undefined,
    days: days ? Number(days) : undefined,
  });

  /** 조건을 바꾸면 첫 페이지로 돌아간다. 3페이지에서 필터를 걸면 결과가 없는 페이지가 뜬다. */
  const change = (apply: () => void) => {
    apply();
    setPage(0);
  };

  const columns = useMemo<ColumnDef<AdminAuditLog, unknown>[]>(
    () => [
      {
        accessorKey: 'createdAt',
        header: '시각',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} />,
      },
      {
        accessorKey: 'actorEmail',
        header: '실행자',
        meta: { width: '15rem' },
        cell: (c) => <span className="font-medium">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'action',
        header: '조치',
        // 폭을 주지 않으면 "격리 메시지 재투입"이 두 줄로 접혀 그 행만 키가 커진다.
        meta: { width: '11rem' },
        // 서버가 남기는 것은 매핑 패턴이라 그대로 찍으면 조치 이력이 서버 로그가 된다.
        // 모르는 값은 원문을 남긴다 - 새 조치가 생긴 것을 화면에서 알아채야 한다.
        cell: (c) => {
          const action = String(c.getValue());
          const label = auditActionLabel(action);
          return label === action ? (
            <span className="font-mono text-xs text-ink-muted">{action}</span>
          ) : (
            <span className="whitespace-nowrap" title={action}>
              {label}
            </span>
          );
        },
      },
      {
        accessorKey: 'targetId',
        header: '대상',
        meta: { width: '10rem' },
        cell: (c) =>
          c.getValue() ? (
            <span className="font-mono text-xs">{String(c.getValue())}</span>
          ) : (
            <span className="text-ink-muted">-</span>
          ),
      },
      {
        accessorKey: 'result',
        header: '결과',
        meta: { width: '7rem' },
        cell: (c) =>
          c.getValue() === 'FAILURE' ? (
            <StatusBadge tone="attention">실패</StatusBadge>
          ) : (
            <StatusBadge tone="muted">성공</StatusBadge>
          ),
      },
      {
        accessorKey: 'detail',
        header: '비고',
        cell: (c) =>
          c.getValue() ? (
            <span className="line-clamp-1 text-xs text-ink-muted">{String(c.getValue())}</span>
          ) : null,
      },
    ],
    [],
  );

  const filtered = Boolean(actorEmail || result || days);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="조치 이력"
        description="관리자가 상태를 바꾼 모든 요청이 남습니다. 조회는 남기지 않습니다. 목록을 열어본 기록까지 쌓으면 실제 조치가 묻힙니다."
      />

      {/* 표를 뒤지기 전에 어디를 볼지 정하는 자리다. 실패가 튄 날을 보고 그 기간을
       * 거는 식으로 쓴다. 그래서 이 그래프들은 아래 필터를 따라가지 않는다. */}
      <Card>
        <CardHeader
          title="일자별 조치"
          description="최근 30일입니다. 아래 필터와 무관하게 전체를 봅니다."
          actions={
            stats.data && (
              <ShareNote value={stats.data.failed} total={stats.data.total} suffix="건이 실패" />
            )
          }
        />
        <CardBody>
          <Loaded query={stats} skeleton={<Skeleton className="h-44" />}>
            {(data) => <DailyChart data={data.daily} series={DAILY_SERIES} height={200} />}
          </Loaded>
        </CardBody>
      </Card>

      {/* 조치 종류와 담당자를 한 카드에 나란히 둔다.
       *
       * 카드를 나누면 줄 수가 아홉과 셋이라 짧은 쪽 바닥에 150px 짜리 빈 자리가 남는다.
       * 둘 다 "최근 30일 조치를 무엇으로 쪼갤 것인가"라는 같은 질문의 두 가지 답이라
       * 한 지붕 아래 두어도 카드의 뜻이 흐려지지 않는다. */}
      <Card>
        <CardHeader
          title="종류와 담당자"
          description="최근 30일 조치를 무엇을 했는지와 누가 했는지로 나눠 봅니다."
        />
        <CardBody className="grid grid-cols-1 gap-x-8 gap-y-6 lg:grid-cols-2">
          <div className="flex flex-col gap-2.5">
            <p className="text-2xs font-semibold uppercase tracking-wide text-ink-muted">종류</p>
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <DistributionBars
                  data={data.actions.map((row) => ({
                    label: auditActionLabel(row.action),
                    count: row.count,
                  }))}
                  emptyTitle="조치가 없습니다"
                  emptyDescription="관리자가 상태를 바꾸면 여기에 쌓입니다."
                />
              )}
            </Loaded>
          </div>

          <div className="flex flex-col gap-2.5">
            <p className="text-2xs font-semibold uppercase tracking-wide text-ink-muted">담당자</p>
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <DistributionBars
                  data={data.actors.map((row) => ({ label: row.actorEmail, count: row.count }))}
                  emptyTitle="조치가 없습니다"
                  emptyDescription="관리자가 상태를 바꾸면 여기에 쌓입니다."
                />
              )}
            </Loaded>
          </div>
        </CardBody>
      </Card>

      <Card>
        <CardHeader
          title="감사 로그"
          description="append-only 입니다. 수정도 삭제도 하지 않습니다."
          actions={
            <div className="flex flex-wrap items-center gap-2">
              <SearchInput
                value={actorInput}
                onChange={(event) => change(() => setActorInput(event.target.value))}
                placeholder="실행자 (일부만)"
                className="w-52"
              />
              <Select
                value={result}
                onChange={(event) =>
                  change(() => setResult(event.target.value as AdminAuditResult | ''))
                }
                className="w-28"
              >
                <option value="">전체 결과</option>
                <option value="SUCCESS">성공</option>
                <option value="FAILURE">실패</option>
              </Select>
              <Select
                value={days}
                onChange={(event) => change(() => setDays(event.target.value))}
                className="w-28"
              >
                {RANGES.map((range) => (
                  <option key={range.label} value={range.value}>
                    {range.label}
                  </option>
                ))}
              </Select>
            </div>
          }
        />
        <DataTable
          error={logs.error}
          onRetry={() => logs.refetch()}
          columns={columns}
          data={logs.data?.content ?? []}
          loading={logs.isPending}
          emptyTitle={filtered ? '조건에 맞는 조치가 없습니다' : '조치 이력이 없습니다'}
          emptyDescription={
            filtered ? '조건을 풀어 보세요.' : '관리자가 무언가를 바꾸면 여기에 남습니다.'
          }
        />
        {logs.data && (
          <Pagination
            page={logs.data.page}
            totalPages={logs.data.totalPages}
            totalElements={logs.data.totalElements}
            onChange={setPage}
          />
        )}
      </Card>
    </div>
  );
}
