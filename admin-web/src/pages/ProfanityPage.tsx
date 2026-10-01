import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import {
  useAuditDecision,
  useNicknameAuditQuality,
  useNicknameAuditStats,
  useNicknameAudits,
} from '@/api/queries';
import type { NicknameAudit, NicknameAuditStatus } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ERROR_SURFACE } from '@/components/ui/errorSurface';
import { Loaded } from '@/components/ui/Loaded';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pagination } from '@/components/ui/Pagination';
import { Tile } from '@/components/ui/Tile';
import { TileGrid, TileSkeletons } from '@/components/ui/TileGrid';
import { Tabs } from '@/components/ui/Tabs';
import { Timestamp } from '@/components/ui/Timestamp';
import { DataTable } from '@/components/DataTable';
import { DailyChart } from '@/components/charts/DailyChart';
import { DistributionBars } from '@/components/charts/DistributionBars';
import { Histogram } from '@/components/charts/Histogram';
import { Skeleton } from '@/components/ui/EmptyState';
import { nicknameAuditStatusLabel } from '@/lib/labels';
import { ProfanityWordsCard } from '@/components/ProfanityWordsCard';
import { formatPercent } from '@/lib/format';

const TABS: { value: NicknameAuditStatus; label: string; hint: string }[] = [
  { value: 'FLAGGED', label: nicknameAuditStatusLabel('FLAGGED'), hint: 'AI가 걸러낸 닉네임' },
  {
    value: 'PENDING',
    label: nicknameAuditStatusLabel('PENDING'),
    hint: 'AI가 판단하지 못한 닉네임',
  },
];

/** 걸린 쪽이 코랄이다. 사람 손이 필요해지는 쪽이라 그 막대가 늘어나는 것을 먼저 봐야 한다. */
const DAILY_SERIES = [
  { key: 'flagged', name: '걸림', color: 'var(--chart-1)' },
  { key: 'passed', name: '통과', color: 'var(--gray-300)' },
];

export function ProfanityPage() {
  const [status, setStatus] = useState<NicknameAuditStatus>('FLAGGED');
  const [page, setPage] = useState(0);

  const audits = useNicknameAudits(status, page);
  const quality = useNicknameAuditQuality(30);
  const stats = useNicknameAuditStats(30);
  const decide = useAuditDecision();

  const columns = useMemo<ColumnDef<NicknameAudit, unknown>[]>(
    () => [
      {
        accessorKey: 'nickname',
        header: '닉네임',
        meta: { width: '12rem' },
        cell: (c) => <span className="font-medium">{String(c.getValue())}</span>,
      },
      {
        id: 'confidence',
        header: 'AI 신뢰도',
        meta: { width: '10rem', align: 'right' },
        accessorFn: (row) => row.confidence?.value ?? 0,
        cell: (c) => {
          const value = Number(c.getValue());
          return (
            <span className="inline-flex items-center justify-end gap-2">
              {/* 막대를 함께 그린다. 숫자만 보면 0.62와 0.91의 차이가 눈에 안 들어온다. */}
              <span className="h-1.5 w-16 overflow-hidden rounded-full bg-subtle">
                <span
                  className="block h-full rounded-full bg-accent"
                  style={{ width: `${Math.round(value * 100)}%` }}
                />
              </span>
              <span className="w-9 text-right tabular-nums">{value.toFixed(2)}</span>
            </span>
          );
        },
      },
      { accessorKey: 'reason', header: '사유' },
      {
        accessorKey: 'createdAt',
        header: '접수',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} />,
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '10rem', align: 'right' },
        cell: (c) => (
          <span className="inline-flex gap-1.5">
            {/* 확인 창을 두지 않는다. 검열 판정은 되돌릴 수 있고(반대 버튼을 누르면 된다)
             * 한 번에 수십 건을 처리하는 화면이라 매번 창이 뜨면 일이 안 된다. */}
            <Button
              size="sm"
              disabled={decide.isPending}
              onClick={() => decide.mutate({ id: c.row.original.id, decision: 'allow' })}
            >
              허용
            </Button>
            <Button
              size="sm"
              variant="danger"
              disabled={decide.isPending}
              onClick={() => decide.mutate({ id: c.row.original.id, decision: 'block' })}
            >
              차단
            </Button>
          </span>
        ),
      },
    ],
    [decide],
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="닉네임 검열"
        description="AI가 걸러낸 닉네임을 사람이 확인합니다. 뒤집힌 비율이 모델을 손볼 시점을 알려줍니다."
      />

      <Loaded
        query={quality}
        errorClassName={ERROR_SURFACE}
        skeleton={
          <TileGrid>
            <TileSkeletons />
          </TileGrid>
        }
      >
        {(data) => (
          <TileGrid>
            <Tile
              label="AI 판정 뒤집힘"
              value={formatPercent(data.overrideRate)}
              hint="최근 30일. 높으면 모델 점검"
            />
            <Tile label="오탐" value={data.falsePositive} hint="AI가 걸렀는데 관리자가 허용" />
            <Tile label="미탐" value={data.falseNegative} hint="AI가 놓쳤는데 관리자가 차단" />
            <Tile label="판정 일치" value={data.agreed} hint={`총 ${data.total}건 중`} />
          </TileGrid>
        )}
      </Loaded>

      {/* 대기 목록은 지금 손이 필요한 것만 보여준다. 목록이 길어진 것이 욕이 늘어서인지
       * 모델이 예민해져서인지는 이 셋을 나란히 봐야 갈린다.
       *
       * 일자별만 한 줄을 통째로 쓴다. 가로축이 서른 칸이라 절반 폭에서는 날짜 눈금이
       * 서로 붙어 언제인지 못 읽는다. 칸이 다섯 이하인 분포 둘은 반씩 나눠도 넉넉하다. */}
      <Card>
        <CardHeader
          title="일자별 검열"
          description="걸림은 사람이 봐야 하는 판정입니다. 최근 30일."
        />
        <CardBody>
          <Loaded query={stats} skeleton={<Skeleton className="h-44" />}>
            {(data) => <DailyChart data={data.daily} series={DAILY_SERIES} height={200} />}
          </Loaded>
        </CardBody>
      </Card>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card className="flex flex-col">
          <CardHeader title="판정 분포" description="닉네임이 어디에서 멈췄는지입니다." />
          <CardBody className="flex flex-1 items-center pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <DistributionBars
                  data={data.statuses.map((slice) => ({
                    label: nicknameAuditStatusLabel(slice.status),
                    count: slice.count,
                  }))}
                  emptyTitle="검열 기록이 없습니다"
                  emptyDescription="닉네임이 만들어지면 여기에 쌓입니다."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader
            title="AI 신뢰도"
            description="걸린 닉네임만 셉니다. 낮은 쪽이 두꺼우면 확신 없이 걸고 있다는 뜻입니다."
          />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <Histogram
                  data={data.confidenceBuckets}
                  emptyTitle="걸린 닉네임이 없습니다"
                  emptyDescription="검열에 걸려야 신뢰도가 남습니다."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader
          title="검열 대기"
          description={TABS.find((tab) => tab.value === status)?.hint}
          actions={
            <Tabs
              tabs={TABS}
              value={status}
              label="검열 상태"
              onChange={(next) => {
                setStatus(next);
                setPage(0);
              }}
            />
          }
        />

        <DataTable
          error={audits.error}
          onRetry={() => audits.refetch()}
          columns={columns}
          data={audits.data?.content ?? []}
          loading={audits.isPending}
          emptyTitle={`${nicknameAuditStatusLabel(status)} 상태의 닉네임이 없습니다`}
          emptyDescription="새 닉네임이 검열에 걸리면 여기에 쌓입니다."
        />
        {audits.data && (
          <Pagination
            page={audits.data.page}
            totalPages={audits.data.totalPages}
            totalElements={audits.data.totalElements}
            onChange={setPage}
          />
        )}
      </Card>

      {/* 검열 큐 아래에 둔다. 같은 화면인 이유는, 큐를 보다가 "이건 사전에 넣자" 하는
       * 순간이 잦기 때문이다. 메뉴를 옮겨 가며 하면 그 흐름이 끊긴다. */}
      <ProfanityWordsCard />
    </div>
  );
}
