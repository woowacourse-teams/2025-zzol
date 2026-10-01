import type { ColumnDef } from '@tanstack/react-table';
import { ArrowUpRight } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useReportBacklog, useReportStats, useReports, useResolveReport } from '@/api/queries';
import type { Report, ReportCategory, ReportStats, ReportStatus } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { ContextPanel, usePanelParam } from '@/components/ui/ContextPanel';
import { ERROR_SURFACE } from '@/components/ui/errorSurface';
import { KeyValue } from '@/components/ui/KeyValue';
import { Loaded } from '@/components/ui/Loaded';
import { Select } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pagination } from '@/components/ui/Pagination';
import { Tile } from '@/components/ui/Tile';
import { TileSkeletons } from '@/components/ui/TileGrid';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Timestamp } from '@/components/ui/Timestamp';
import { DataTable } from '@/components/DataTable';
import { DailyChart } from '@/components/charts/DailyChart';
import { DistributionBars } from '@/components/charts/DistributionBars';
import { DonutChart } from '@/components/charts/DonutChart';
import { Skeleton } from '@/components/ui/EmptyState';
import { formatDurationMinutes } from '@/lib/format';
import { miniGameLabel, reportCategoryLabel } from '@/lib/labels';

/**
 * 유형 필터가 고를 수 있는 값.
 *
 * <p>표시할 말은 여기 적지 않고 {@link reportCategoryLabel} 이 낸다. 두 곳에 적어 두면
 * 목록의 "건의" 와 필터의 "건의사항" 처럼 같은 값이 화면에서 다른 이름으로 불린다.
 */
const CATEGORIES: ReportCategory[] = ['BUG', 'SUGGESTION', 'GAME_REQUEST', 'OTHER'];

/** 접수가 코랄이다. 들어오는 일이 주인공이고, 처리는 그것을 얼마나 따라갔는지를 보여준다. */
const DAILY_SERIES = [
  { key: 'received', name: '접수', color: 'var(--chart-1)' },
  { key: 'resolved', name: '처리', color: 'var(--gray-300)' },
];

/**
 * 게임과 무관한 신고를 순위에서 뺀다.
 *
 * <p>건의와 기타는 대개 게임에 붙지 않아 이 칸 하나가 전체의 3분의 2를 먹는다. 남겨 두면
 * 1위가 "게임 아님"이 되고, 막대는 1위 대비 길이라 나머지 게임이 전부 실선처럼 얇아진다.
 * 이 카드가 묻는 것은 <b>어느 게임에서 신고가 나오는가</b>지 게임 신고가 전체의 몇
 * 퍼센트인가가 아니다. 그 숫자는 카드 설명에 한 줄로 적는다.
 */
function gameRanking(stats: ReportStats) {
  return stats.games
    .filter((row) => row.gameType !== null)
    .map((row) => ({ label: row.label as string, count: row.count }));
}

function gameCardDescription(stats: ReportStats | undefined) {
  if (!stats) {
    return '게임과 무관한 신고는 빠집니다.';
  }
  const other = stats.games.find((row) => row.gameType === null)?.count ?? 0;
  return `게임과 무관한 신고 ${other}건은 뺐습니다. 비중은 게임 신고끼리의 비중입니다.`;
}

export function ReportsPage() {
  const [status, setStatus] = useState<ReportStatus | ''>('PENDING');
  const [category, setCategory] = useState<ReportCategory | ''>('');
  const [page, setPage] = useState(0);
  const [target, setTarget] = useState<Report | null>(null);

  const reports = useReports({
    status: status || undefined,
    category: category || undefined,
    page,
  });

  /** 조건을 바꾸면 첫 페이지로 돌아간다. 3페이지에서 필터를 걸면 결과가 없는 페이지가 뜬다. */
  const change = (apply: () => void) => {
    apply();
    setPage(0);
  };
  const backlog = useReportBacklog();
  const stats = useReportStats(30);
  const resolve = useResolveReport();

  // 열린 신고를 주소에 남긴다. 조사하다 찾은 건을 링크로 넘길 수 있어야 한다.
  const panel = usePanelParam('report');
  const selected =
    reports.data?.content.find((report) => String(report.id) === panel.value) ?? null;

  const columns = useMemo<ColumnDef<Report, unknown>[]>(
    () => [
      {
        accessorKey: 'id',
        header: 'ID',
        meta: { width: '4rem' },
        cell: (c) => (
          <span className="font-mono text-xs text-ink-muted">{String(c.getValue())}</span>
        ),
      },
      {
        accessorKey: 'category',
        header: '카테고리',
        meta: { width: '7rem' },
        cell: (c) => reportCategoryLabel(String(c.getValue())),
      },
      {
        accessorKey: 'content',
        header: '내용',
        cell: (c) => <span className="line-clamp-2">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'joinCode',
        header: '방',
        meta: { width: '6rem' },
        cell: (c) =>
          c.getValue() ? (
            <span className="font-mono text-xs">{String(c.getValue())}</span>
          ) : (
            <span className="text-ink-muted">-</span>
          ),
      },
      {
        accessorKey: 'status',
        header: '상태',
        meta: { width: '7rem' },
        cell: (c) =>
          c.getValue() === 'PENDING' ? (
            <StatusBadge tone="attention">미처리</StatusBadge>
          ) : (
            <StatusBadge tone="muted">처리 완료</StatusBadge>
          ),
      },
      {
        accessorKey: 'createdAt',
        header: '접수',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} />,
      },
      // 행마다 있던 "처리" 버튼을 걷어냈다.
      //
      // 스무 행에 같은 버튼이 서면 열 하나가 통째로 버튼이 되어 표를 읽는 내내 눈이
      // 그쪽에 끌린다. 그보다 큰 이유는 따로 있다. 그 버튼은 <b>내용을 안 읽고도</b>
      // 처리 완료를 누를 수 있게 했다. 신고 처리는 읽고 나서 하는 일이라, 행을 열면
      // 전문이 보이고 거기서 처리하는 흐름이 맞다.
    ],
    [],
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="신고"
        description="접수된 신고를 확인하고 처리합니다. 그래프는 최근 30일 기준입니다."
      />

      {/* 지금 밀린 신고 둘과 일자별 흐름을 한 줄에 둔다.
       *
       * <p>한때 이 줄은 타일 넷이었다. 처리 시간의 중앙값과 p95 가 뒤의 둘이었는데
       * 걷어냈다. 백분위는 표본이 쌓여야 뜻이 생기는 수인데 신고는 하루에 몇 건이라,
       * 한 건이 들어오고 나갈 때마다 두 수가 크게 흔들렸다. 흔들리는 수가 한 줄에 서
       * 있으면 옆의 미처리 건수까지 못 믿게 된다.
       *
       * <p>남은 둘만으로 줄을 채우면 오른쪽 절반이 빈다. 그래서 일자별 그래프를 이 줄로
       * 올렸다. "지금 밀렸나"와 "요즘 얼마나 들어오나"는 같은 자리에서 묻는 질문이다. */}
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
        {/* 실패하면 "-" 대신 실패했다고 말한다. "-" 는 "오늘 0건"과 똑같이 생겨서,
         * 서버가 답을 못 준 것을 처리할 게 없는 것으로 읽게 만든다. */}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1 lg:grid-rows-2">
          <Loaded
            query={backlog}
            errorClassName={ERROR_SURFACE}
            skeleton={<TileSkeletons count={2} />}
          >
            {(data) => (
              <>
                <Tile
                  label="미처리"
                  value={data.pendingCount}
                  // 이 칸만 hint 가 없어 나란히 선 둘 중 첫 칸의 아래가 비어 있었다.
                  // 타일은 라벨, 값, 보조 문구 세 줄이 한 벌이라 하나만 두 줄이면 어긋나 보인다.
                  hint="지금 손대야 할 건수"
                />
                <Tile
                  label="가장 오래 기다린 건"
                  value={formatDurationMinutes(data.oldestPendingMinutes)}
                  hint="접수 후 경과"
                />
              </>
            )}
          </Loaded>
        </div>

        <Card className="flex flex-col">
          <CardHeader
            title="일자별 접수와 처리"
            description="처리는 접수일이 아니라 처리한 날로 셉니다."
          />
          {/* 높이를 못 박는다. 왼쪽 타일 둘이 이 카드 높이에 맞춰 늘어나는데, 그래프가
            * 기본 높이(180)면 타일 하나가 160px 이 되어 숫자 아래가 휑해진다. 140 이면
            * 타일이 제 높이(약 100)로 선다. 28개 막대는 이 높이에서도 읽힌다. */}
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-[8.75rem]" />}>
              {(data) => <DailyChart data={data.daily} series={DAILY_SERIES} height={140} />}
            </Loaded>
          </CardBody>
        </Card>
      </div>

      {/* 무엇 때문에 신고가 들어오는가. 목록과 SLA 타일이 둘 다 답하지 못하는 질문이다.
       * 신고의 절반이 한 게임에서 나오면 그건 신고 처리로 풀 일이 아니다. */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card className="flex flex-col">
          <CardHeader title="카테고리" description="최근 30일에 접수된 신고입니다." />
          <CardBody className="flex flex-1 items-center pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <DonutChart
                  slices={data.categories.map((slice) => ({
                    label: reportCategoryLabel(slice.category),
                    count: slice.count,
                  }))}
                  centerLabel="신고"
                />
              )}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader title="게임별 신고" description={gameCardDescription(stats.data)} />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <DistributionBars
                  ranked
                  data={gameRanking(data)}
                  emptyTitle="게임에 붙은 신고가 없습니다"
                  emptyDescription="버그 신고에는 게임이 함께 들어옵니다."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader
          title="신고 목록"
          description="행을 누르면 전문과 처리가 옆에서 열립니다."
          actions={
            <div className="flex flex-wrap items-center gap-2">
              <Select
                value={status}
                onChange={(event) =>
                  change(() => setStatus(event.target.value as ReportStatus | ''))
                }
                className="w-32"
              >
                <option value="">전체 상태</option>
                <option value="PENDING">미처리</option>
                <option value="RESOLVED">처리 완료</option>
              </Select>
              {/* 유형은 상태 다음이다. 운영자가 먼저 정하는 것은 "지금 손댈 것만 볼까"이고,
                * 유형은 그 안에서 다시 좁히는 조건이다. */}
              <Select
                value={category}
                onChange={(event) =>
                  change(() => setCategory(event.target.value as ReportCategory | ''))
                }
                className="w-32"
              >
                <option value="">전체 유형</option>
                {CATEGORIES.map((value) => (
                  <option key={value} value={value}>
                    {reportCategoryLabel(value)}
                  </option>
                ))}
              </Select>
            </div>
          }
        />

        <DataTable
          error={reports.error}
          onRetry={() => reports.refetch()}
          columns={columns}
          data={reports.data?.content ?? []}
          loading={reports.isPending}
          onRowClick={(report) => panel.open(String(report.id))}
          isRowSelected={(report) => String(report.id) === panel.value}
          emptyTitle={emptyListTitle(status, category)}
          emptyDescription={
            category ? '조건을 풀어 보세요.' : '새 신고가 들어오면 여기에 쌓입니다.'
          }
        />
        {reports.data && (
          <Pagination
            page={reports.data.page}
            totalPages={reports.data.totalPages}
            totalElements={reports.data.totalElements}
            onChange={setPage}
          />
        )}
      </Card>

      <ReportPanel
        report={selected}
        onClose={panel.close}
        onResolve={() => selected && setTarget(selected)}
      />

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => !open && setTarget(null)}
        destructive={false}
        title="이 신고를 처리 완료로 표시할까요?"
        description="처리 완료로 바꾸면 미처리 목록에서 빠집니다. 되돌리는 기능은 없습니다."
        target={target ? `#${target.id} · ${target.content}` : undefined}
        confirmLabel="처리 완료로 표시"
        pending={resolve.isPending}
        onConfirm={() => {
          if (!target) return;
          resolve.mutate(target.id, {
            onSuccess: () => {
              setTarget(null);
              panel.close();
            },
          });
        }}
      />
    </div>
  );
}

/**
 * 목록이 비었을 때의 제목. 걸어 둔 조건을 그대로 되읽어 준다.
 *
 * <p>"신고가 없습니다" 한 문장으로 두면, 유형을 걸어 놓은 것을 잊은 운영자가 신고가 한
 * 건도 없다고 믿고 화면을 닫는다. 조건이 제목에 적혀 있으면 빈 목록이 곧 단서가 된다.
 */
function emptyListTitle(status: ReportStatus | '', category: ReportCategory | '') {
  const state = { PENDING: '미처리 ', RESOLVED: '처리 완료된 ', '': '' }[status];
  const kind = category ? `${reportCategoryLabel(category)} ` : '';
  return `${state}${kind}신고가 없습니다`;
}

/**
 * 신고 한 건의 전문과 조치.
 *
 * <p>목록이 못 보여주는 셋을 담는다. 잘리지 않은 내용, 신고자 IP, 그리고 <b>그 방으로
 * 가는 링크</b>다. 마지막이 특히 빠져 있었다. 표에 joinCode 가 찍혀 있는데 눌러지지
 * 않아서, 신고를 읽고 그 방을 보려면 방 조회로 가서 코드를 다시 입력해야 했다.
 */
function ReportPanel({
  report,
  onClose,
  onResolve,
}: {
  report: Report | null;
  onClose: () => void;
  onResolve: () => void;
}) {
  return (
    <ContextPanel
      open={report !== null}
      onClose={onClose}
      title={report ? `신고 #${report.id}` : ''}
      description={report ? reportCategoryLabel(report.category) : undefined}
      footer={
        report?.status === 'PENDING' ? (
          <Button variant="primary" onClick={onResolve}>
            처리 완료로 표시
          </Button>
        ) : undefined
      }
    >
      {report && (
        <div className="flex flex-col gap-5">
          {/* 내용을 맨 위에 크게 둔다. 운영자가 이 패널을 여는 이유가 이것 하나다.
           * 메타데이터를 위에 깔면 매번 그것을 지나쳐 아래로 내려가야 한다. */}
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-ink">{report.content}</p>

          {/* 없는 것은 칸도 만들지 않는다.
           *
           * 건의 신고에는 게임도 방도 없고 미처리 건에는 처리 시각이 없다. 그대로 두면
           * 여섯 칸 중 넷이 "-" 로 서서, 화면이 값을 못 불러온 것처럼 보인다. 빈 칸이
           * 늘어선 패널은 조회가 실패한 패널과 똑같이 생겼다.
           *
           * 신고자 IP 만 예외로 "-" 를 남긴다. 그 자리는 값이 있어야 정상이라 비어 있다는
           * 사실 자체가 정보다. 칸을 지우면 원래 그런 신고인 줄 알게 된다. */}
          <KeyValue
            items={[
              {
                label: '상태',
                value:
                  report.status === 'PENDING' ? (
                    <StatusBadge tone="attention">미처리</StatusBadge>
                  ) : (
                    <StatusBadge tone="muted">처리 완료</StatusBadge>
                  ),
              },
              { label: '접수', value: <Timestamp value={report.createdAt} /> },
              ...(report.resolvedAt
                ? [{ label: '처리', value: <Timestamp value={report.resolvedAt} /> }]
                : []),
              ...(report.gameType
                ? [{ label: '게임', value: miniGameLabel(report.gameType) }]
                : []),
              ...(report.joinCode
                ? [
                    {
                      label: '방',
                      value: (
                        <Link
                          // 옛 주소(/trace)를 거치지 않는다. 리다이렉트가 한 번 더 도는
                          // 것도 그렇고, 지금 있는 화면으로 곧장 보내는 편이 읽기도 쉽다.
                          to={`/rooms?q=${encodeURIComponent(report.joinCode)}`}
                          className="inline-flex items-center gap-1 font-mono text-xs text-ink underline decoration-border-strong underline-offset-2 hover:decoration-ink"
                        >
                          {report.joinCode}
                          <ArrowUpRight className="size-3 text-ink-muted" aria-hidden />
                        </Link>
                      ),
                    },
                  ]
                : []),
              {
                label: '신고자 IP',
                value: report.ip ? <span className="font-mono text-xs">{report.ip}</span> : null,
              },
            ]}
          />
        </div>
      )}
    </ContextPanel>
  );
}
