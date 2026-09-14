import type { ColumnDef } from '@tanstack/react-table';
import {
  DoorOpen,
  Flag,
  Hourglass,
  MessageSquareWarning,
  ServerCog,
  ShieldBan,
  SpellCheck,
  UserPlus,
  Users,
} from 'lucide-react';
import { lazy, Suspense, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useActionQueue,
  useDailySummary,
  useGamePlayStats,
  useInbox,
  usePeriodSummary,
  useProviderStats,
  useTrend,
} from '@/api/queries';
import type { DailyTrend, Funnel, InboxItem, InboxKind } from '@/api/types';
import { ActionQueueStrip } from '@/components/ActionQueueStrip';
import { FunnelBar } from '@/components/FunnelBar';
import { TrendLegend } from '@/components/TrendLegend';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/EmptyState';
import { ERROR_SURFACE } from '@/components/ui/errorSurface';
import { Loaded } from '@/components/ui/Loaded';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { Tile } from '@/components/ui/Tile';
import { TileGrid, TileSkeletons } from '@/components/ui/TileGrid';
import { Timestamp } from '@/components/ui/Timestamp';
import { DataTable } from '@/components/DataTable';
import { DistributionBars } from '@/components/charts/DistributionBars';
import { toGameRowsWithDropOff } from '@/lib/gameStats';
import { formatNumber, formatPercent } from '@/lib/format';
import { inboxKindLabel, reportCategoryLabel } from '@/lib/labels';

const TrendChart = lazy(() =>
  import('@/components/TrendChart').then((module) => ({ default: module.TrendChart })),
);

/** 도넛도 recharts 다. 같은 이유로 떼어 받는다. */
const ProviderShare = lazy(() =>
  import('@/components/ProviderShare').then((module) => ({ default: module.ProviderShare })),
);

/**
 * 좌우 분할은 둘뿐이다. 같은 급의 질문이면 반반, 한쪽이 본문이고 다른 쪽이 곁들이면 2:1.
 *
 * <p><b>높이를 맞춘다.</b> 한때 {@code items-start} 로 각자 내용만큼만 차지하게 뒀는데,
 * 나란히 선 카드의 아래 모서리가 제각각이라 판이 어긋나 보였다. 늘리되 짧은 카드가 빈 판이
 * 되지 않게 안에서 남는 높이를 흡수한다 - 목록은 줄 사이로 나눠 가지고, 퍼널은 가운데 선다.
 */
const SPLIT_WIDE = 'grid gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(17rem,1fr)]';
const SPLIT_HALF = 'grid gap-4 xl:grid-cols-2';

/**
 * 작업함에 보여 줄 줄 수.
 *
 * <p>서버는 스무 건을 주는데 여기서는 여덟 줄만 깐다. 종류별 건수는 칩이 스무 건 전부를
 * 세고, 전체를 훑을 일이면 각 화면으로 가면 된다.
 */
const PREVIEW_ROWS = 7;

type KindFilter = InboxKind | 'ALL';

/**
 * 홈.
 *
 * <p>네 줄로 답한다. <b>오늘 얼마나 돌았나, 흐름이 평소 같나, 지금 손댈 게 무엇인가,
 * 무엇으로 노는가.</b> 위에서 아래로 갈수록 시간 단위가 길어진다. 오늘, 2주, 지금, 한 달.
 *
 * <p>덩어리 여덟 개를 얹어 4700px 이 됐다가 세 개로 줄여 2000px 이 된 적이 있다. 둘 다
 * 아니었다. 여덟 개는 무엇부터 봐야 하는지가 사라졌고, 세 개는 서비스가 무엇으로 돌아가는지를
 * 못 보여줬다. 첫 화면은 <b>이 서비스의 요약</b>이라 규모와 구성이 빠지면 요약이 아니다.
 *
 * <p>담는 지표의 기준은 그대로다. <b>Grafana 가 못 보는 것.</b> 응답시간, 에러율, JVM 은
 * 그쪽이 이미 본다. 여기 있는 것은 전부 도메인 조인이거나 우리 DB 에만 있는 기록이다.
 */
export function HomePage() {
  // 기간은 훅 호출보다 먼저 정해져야 한다. 구성과 퍼널 조회가 이 값을 받는다.
  const [days, setDays] = useState(30);

  const queue = useActionQueue();
  const inbox = useInbox();
  const summary = useDailySummary();
  const trend = useTrend(14);
  const period = usePeriodSummary(days);
  const games = useGamePlayStats(days);
  const providers = useProviderStats();

  const [kind, setKind] = useState<KindFilter>('ALL');

  const series = trend.data ?? [];
  const items = inbox.data ?? [];
  const filtered = kind === 'ALL' ? items : items.filter((item) => item.kind === kind);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="홈"
        description="오늘 서비스가 얼마나 돌았고 지금 손댈 게 무엇인지. 인프라 지표는 Grafana(status.zzol.site)가 봅니다."
        actions={<Tabs tabs={RANGES} value={days} onChange={setDays} label="조회 기간" />}
      />

      {/* 오늘 숫자에 어제 대비 증감과 스파크라인이 붙는다. 숫자 하나로는 그 값이 평소보다
       * 높은지 낮은지를 알 수 없는데, 아침에 홈을 여는 사람이 묻는 것은 그것이다. */}
      <Loaded
        query={summary}
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
              label="오늘 방 생성"
              icon={DoorOpen}
              value={data.funnel.created}
              hint="오늘 만들어진 방"
              delta={deltaOf(series, 'created')}
              trend={pick(series, 'created')}
            />
            <Tile
              label="오늘 완주"
              icon={Flag}
              value={data.funnel.completed}
              hint={`생성 대비 ${formatPercent(data.funnel.completionRate, 0)}`}
              delta={deltaOf(series, 'completed')}
              trend={pick(series, 'completed')}
            />
            <Tile
              label="오늘 참여자"
              icon={Users}
              value={data.players}
              hint="여러 방 참여 시 중복"
              delta={deltaOf(series, 'players')}
              trend={pick(series, 'players')}
            />
            {/* 가입은 일자별 계열이 없다. 빌려온 모양은 이 지표의 흐름이 아니다. */}
            <Tile
              label="오늘 신규 가입"
              icon={UserPlus}
              value={data.signups}
              hint="비회원도 게임은 가능"
            />
          </TileGrid>
        )}
      </Loaded>

      <div className={SPLIT_WIDE}>
        <Card className="flex flex-col">
          <CardHeader title="최근 14일" actions={<TrendLegend />} />
          <CardBody className="flex flex-1 flex-col justify-center">
            <Loaded query={trend} skeleton={<Skeleton className="h-[220px]" />}>
              {(data) => (
                <Suspense fallback={<Skeleton className="h-[220px]" />}>
                  <TrendChart data={data} />
                </Suspense>
              )}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader title="처리 대기" description="막대는 다섯 중 가장 많은 큐 기준입니다." />
          <div className="flex flex-1 flex-col">
            <Loaded
              query={queue}
              skeleton={
                <div className="flex flex-1 flex-col justify-between gap-2 px-5 pb-5">
                  {Array.from({ length: 5 }).map((_, index) => (
                    <Skeleton key={index} className="h-8" />
                  ))}
                </div>
              }
            >
              {(data) => (
                <ActionQueueStrip
                  items={[
                    // 맨 앞이다. 나머지 넷은 사람이 판단해 줄 일이고 쌓이는 게 정상이지만,
                    // 이건 시스템이 멈춘 것이라 0이 아닌 순간 먼저 봐야 한다.
                    {
                      label: '격리 메시지',
                      count: data.deadLetters,
                      to: '/system',
                      icon: ServerCog,
                      critical: true,
                    },
                    {
                      label: '미처리 신고',
                      count: data.pendingReports,
                      to: '/reports',
                      icon: MessageSquareWarning,
                    },
                    // 화면에 FLAGGED, PENDING 을 그대로 적지 않는다. 서버 enum 이름이고
                    // 운영자가 쓰는 말이 아니다.
                    {
                      label: 'AI가 걸러낸 닉네임',
                      count: data.flaggedNicknames,
                      to: '/profanity',
                      icon: SpellCheck,
                    },
                    {
                      label: 'AI가 판단 못한 닉네임',
                      count: data.pendingNicknames,
                      to: '/profanity',
                      icon: Hourglass,
                    },
                    { label: '차단 IP', count: data.blockedIps, to: '/ip-blocks', icon: ShieldBan },
                  ]}
                />
              )}
            </Loaded>
          </div>
        </Card>
      </div>

      {/* 둘 다 "무엇이 얼마나"를 막대로 말하는 목록이라 높이가 비슷하다. 짝을 지을 때
       * 의미만 보고 묶으면 한쪽 카드 안이 200px 넘게 비는 일이 생긴다.
       *
       * 셋을 한 줄에 세워 봤다가 되돌렸다. 소셜 제공자까지 끼우니 칸이 3분의 1로 좁아져
       * 막대가 전부 뭉툭한 토막이 되고, 이름 칸이 고정 폭이라 그 왼쪽이 통째로 비었다.
       * 빈 자리를 없애려다 세 카드를 다 망가뜨린 셈이다. */}
      <div className={SPLIT_HALF}>
        <Card className="flex flex-col">
          <CardHeader
            title="방 진행 퍼널"
            description="게임 시작과 미니게임 완료의 차이가 하다가 나간 방입니다."
          />
          {/* 막대 위에 요약 두 칸을 얹는다.
           *
           * 한때 다섯 단계만 있었고, 옆 카드보다 짧아 생긴 빈 자리를 단계 사이 간격으로
           * 벌려 메웠다. 그건 공백을 여백으로 위장한 것이지 채운 것이 아니다.
           *
           * 이 카드가 답해야 할 질문은 <b>어디서 제일 많이 빠지나</b>인데, 그 답이 오른쪽
           * 끝 -19 다섯 개를 눈으로 견줘야 나왔다. 위로 올려 숫자로 적으면 카드가 내용으로
           * 차고 질문에도 바로 답한다. */}
          <CardBody className="flex flex-1 flex-col gap-4">
            <Loaded query={period} skeleton={<RowSkeleton rows={6} height="h-7" />}>
              {(data) => {
                const stages = funnelStages(data.funnel);
                const worst = worstDrop(stages);
                return (
                  <>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="rounded-md border border-border-default px-4 py-3">
                        <p className="text-xs text-ink-secondary">완주율</p>
                        <p className="mt-1.5 text-xl font-bold leading-none tracking-metric text-ink">
                          {formatPercent(data.funnel.completionRate)}
                        </p>
                        <p className="mt-1.5 text-2xs text-ink-muted">
                          {formatNumber(data.funnel.created)}개 중{' '}
                          {formatNumber(data.funnel.completed)}개
                        </p>
                      </div>
                      <div className="rounded-md border border-border-default px-4 py-3">
                        <p className="text-xs text-ink-secondary">가장 많이 빠지는 구간</p>
                        <p className="mt-1.5 text-xl font-bold leading-none tracking-metric text-ink">
                          {worst === null ? '-' : `${formatNumber(worst.dropped)}개`}
                        </p>
                        <p className="mt-1.5 truncate text-2xs text-ink-muted">
                          {worst === null ? '이탈 없음' : `${worst.from} 다음`}
                        </p>
                      </div>
                    </div>

                    <FunnelBar stages={stages} />
                  </>
                );
              }}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader
            title="게임별 플레이"
            description="시작한 판을 셉니다. 막대의 옅은 꼬리와 오른쪽 숫자가 이탈입니다."
          />
          <div className="flex flex-1 flex-col justify-center">
            <Loaded
              query={games}
              skeleton={
                <div className="flex flex-col gap-2.5 px-5 pb-5">
                  {Array.from({ length: 8 }).map((_, index) => (
                    <Skeleton key={index} className="h-7" />
                  ))}
                </div>
              }
            >
              {(data) => (
                <DistributionBars
                  ranked
                  className="px-5 pb-5"
                  data={toGameRowsWithDropOff(data)}
                  emptyTitle="시작된 게임이 없습니다"
                  emptyDescription="방이 만들어지고 게임이 시작돼야 집계됩니다."
                />
              )}
            </Loaded>
          </div>
        </Card>
      </div>

      {/* 표가 여덟 줄이던 때는 옆 카드가 그만큼 늘어나 도넛 하나가 600px 짜리 카드
       * 한가운데 떠 있었다. 가운데 정렬이라 위아래가 똑같이 비어 배치처럼 보였지만,
       * 비는 양이 그만큼이면 그건 배치가 아니다.
       *
       * 표를 여섯 줄로 줄이고 도넛을 위아래로 쌓아 둘의 키를 맞췄다. 목록에서 여섯 건을
       * 보고 나면 어차피 해당 화면으로 넘어간다. */}
      <div className={SPLIT_WIDE}>
        <InboxCard
          items={items}
          filtered={filtered}
          kind={kind}
          onKindChange={setKind}
          loading={inbox.isPending}
          error={inbox.error}
          onRetry={() => inbox.refetch()}
        />
        <Card className="flex flex-col">
          <CardHeader title="소셜 제공자" description="연결 기준입니다. 회원 수와 다릅니다." />
          <CardBody className="flex flex-1 flex-col justify-center">
            <Loaded query={providers} skeleton={<Skeleton className="h-[200px]" />}>
              {(data) => (
                <Suspense fallback={<Skeleton className="h-[200px]" />}>
                  <ProviderShare stats={data} layout="column" />
                </Suspense>
              )}
            </Loaded>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}

/**
 * 기간 선택지.
 *
 * <p>날짜 범위 피커를 두지 않았다. 실제로 묻는 것은 "이번 주 어때", "지난달 대비 어때"
 * 수준이고, 임의 구간이 필요한 분석은 어차피 SQL 로 간다. 90일이 상한이다 - 서버는 365까지
 * 받지만 백오피스 한 번 열자고 운영 DB 를 길게 잡을 이유가 없다.
 *
 * <p>이 탭이 지배하는 것은 퍼널과 게임별 비중 <b>둘뿐</b>이다. 오늘 칸과 14일 추이는 각자
 * 정해진 기간이 있다. 탭을 옮겼는데 화면 절반만 바뀌는 것이 이상해 보일 수 있지만,
 * 그 둘의 제목이 기간을 직접 적고 있어 헷갈릴 자리가 없다.
 */
const RANGES = [7, 14, 30, 90].map((days) => ({ value: days, label: `${days}일` }));

/**
 * 통합 작업함.
 *
 * <p>옆 칸들이 "어디에 몇 건"을 말한다면 여기는 <b>그게 무엇인지</b>를 말한다. 숫자만
 * 있으면 화면을 옮겨야 무슨 일인지 알 수 있고, 세 종류면 세 화면을 왕복해야 한다.
 */
function InboxCard({
  items,
  filtered,
  kind,
  onKindChange,
  loading,
  error,
  onRetry,
}: {
  items: InboxItem[];
  filtered: InboxItem[];
  kind: KindFilter;
  onKindChange: (kind: KindFilter) => void;
  loading: boolean;
  error: unknown;
  onRetry: () => void;
}) {
  const navigate = useNavigate();

  const columns = useMemo<ColumnDef<InboxItem, unknown>[]>(
    () => [
      {
        accessorKey: 'kind',
        header: '종류',
        meta: { width: '5.5rem' },
        cell: (c) => <KindBadge kind={c.getValue() as InboxKind} />,
      },
      {
        accessorKey: 'title',
        header: '내용',
        cell: (c) => <span className="line-clamp-2">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'detail',
        header: '사유',
        // 주인공은 내용 열이다. 사유에는 대개 "버그" 같은 한 단어가 들어가는데
        // 곁들이는 열이 넓으면 본문이 밀려 두 줄로 감긴다.
        meta: { width: '10rem' },
        cell: (c) => {
          const item = c.row.original;
          const detail = c.getValue() as string | null;
          if (!detail) {
            return <span className="text-ink-muted">-</span>;
          }
          const text = item.kind === 'REPORT' ? reportCategoryLabel(detail) : detail;
          return (
            <span className="line-clamp-2 text-ink-secondary" title={text}>
              {text}
            </span>
          );
        },
      },
      {
        accessorKey: 'occurredAt',
        header: '들어온 시각',
        meta: { width: '7rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} relativeOnly />,
      },
    ],
    [],
  );

  return (
    <Card className="flex flex-col">
      <CardHeader
        title="처리할 일"
        description={`세 곳에서 모은 최신 ${items.length}건 중 앞 ${Math.min(filtered.length, PREVIEW_ROWS)}건입니다.`}
        actions={
          <Tabs
            label="종류"
            value={kind}
            onChange={onKindChange}
            tabs={[
              { value: 'ALL', label: `전체 ${items.length}` },
              { value: 'REPORT', label: `신고 ${count(items, 'REPORT')}` },
              { value: 'NICKNAME', label: `닉네임 ${count(items, 'NICKNAME')}` },
              { value: 'DEAD_LETTER', label: `격리 ${count(items, 'DEAD_LETTER')}` },
            ]}
          />
        }
      />

      {/* 행을 누르면 그 종류의 화면으로 가되 해당 항목이 열린 상태로 간다.
       *
       * 여기서 바로 처리하지 않는 이유가 있다. 세 종류의 조치가 서로 다르고(신고는
       * 처리 완료, 닉네임은 허용과 차단, 격리는 재투입과 폐기), 격리는 원문을 읽지
       * 않고 누르면 안 되는 조치다. 조치 UI 세 벌을 여기 겹쳐 놓으면 작업함이 세 화면을
       * 합친 것이 아니라 <b>네 번째 화면</b>이 된다. */}
      <DataTable
        columns={columns}
        data={filtered.slice(0, PREVIEW_ROWS)}
        loading={loading}
        error={error}
        onRetry={onRetry}
        skeletonRows={PREVIEW_ROWS}
        onRowClick={(item) => navigate(routeOf(item))}
        emptyTitle={kind === 'ALL' ? '처리할 일이 없습니다' : '이 종류는 없습니다'}
        emptyDescription={
          kind === 'ALL'
            ? '새 신고나 검열 대기가 생기면 여기에 쌓입니다.'
            : '다른 종류를 눌러 보세요.'
        }
      />
    </Card>
  );
}

/**
 * 종류 표식.
 *
 * <p>격리만 색이 붙는다. 신고와 검열은 평소에도 쌓이는 것이 정상이라 늘 코랄이면 그 색이
 * 뜻을 잃는다. 격리는 평소 0이고 1이 되는 순간이 곧 사고다.
 */
function KindBadge({ kind }: { kind: InboxKind }) {
  return (
    <StatusBadge tone={kind === 'DEAD_LETTER' ? 'attention' : 'neutral'}>
      {inboxKindLabel(kind)}
    </StatusBadge>
  );
}

/**
 * 행을 눌렀을 때 갈 곳.
 *
 * <p>격리 메시지의 식별자는 {@code OUTBOX:3} 처럼 출처가 앞에 붙는다. 두 테이블의 id 가
 * 겹쳐서 숫자만으로는 무엇을 폐기할지 정해지지 않기 때문이다.
 */
function routeOf(item: InboxItem): string {
  if (item.kind === 'REPORT') {
    return `/reports?open=report:${item.id}`;
  }
  if (item.kind === 'NICKNAME') {
    return '/profanity';
  }
  return '/system';
}

function count(items: InboxItem[], kind: InboxKind): number {
  return items.filter((item) => item.kind === kind).length;
}

/** 퍼널 다섯 단계. 카드와 요약 계산이 같은 목록을 봐야 어긋나지 않는다. */
function funnelStages(funnel: Funnel) {
  return [
    { label: '방 생성', count: funnel.created },
    { label: '게임 시작', count: funnel.gameStarted },
    { label: '미니게임 완료', count: funnel.miniGamePlayed },
    { label: '룰렛 도달', count: funnel.rouletteReached },
    { label: '완주', count: funnel.completed },
  ];
}

/**
 * 가장 많이 빠진 구간.
 *
 * <p>비율이 아니라 <b>개수</b>로 고른다. 운영자가 읽는 말은 "열아홉 개 방이 게임도 못
 * 시작했다"이지 "11퍼센트가 이탈했다"가 아니다. 비율은 분모가 작을수록 커져서, 뒤쪽
 * 단계의 작은 이탈이 앞쪽의 큰 이탈보다 커 보인다.
 *
 * <p>같은 값이면 앞 단계를 고른다. 앞에서 빠진 방은 뒤 단계에 아예 오지 못하므로 먼저
 * 손볼 곳도 앞쪽이다.
 */
function worstDrop(stages: { label: string; count: number }[]) {
  let worst: { from: string; dropped: number } | null = null;
  for (let index = 1; index < stages.length; index += 1) {
    const dropped = (stages[index - 1]?.count ?? 0) - (stages[index]?.count ?? 0);
    if (dropped > 0 && (worst === null || dropped > worst.dropped)) {
      worst = { from: stages[index - 1]?.label ?? '', dropped };
    }
  }
  return worst;
}

/** 추이 응답에서 계열 하나만 뽑는다. 스파크라인은 값 배열만 받는다. */
function pick(series: DailyTrend[], key: 'created' | 'completed' | 'players') {
  return series.map((day) => day[key]);
}

/**
 * 어제 대비 증감.
 *
 * <p>스파크라인이 모양을 말한다면 이 숫자는 <b>얼마나 달라졌는지</b>를 말한다. 아침에 홈을
 * 여는 사람이 묻는 것이 그것이라, 그림만으로는 "조금 줄었다"까지밖에 못 읽는다.
 *
 * <p>어제가 없으면 아무것도 돌려주지 않는다. 0을 돌려주면 {@code Tile} 이 "변화 없음"으로
 * 그리는데, 그건 잰 적이 없는 것과 다른 말이다.
 */
function deltaOf(series: DailyTrend[], key: 'created' | 'completed' | 'players') {
  if (series.length < 2) {
    return undefined;
  }
  const today = series[series.length - 1]?.[key] ?? 0;
  const yesterday = series[series.length - 2]?.[key] ?? 0;
  return today - yesterday;
}

/** 목록형 카드의 로딩 자리. 좌우 여백은 CardBody 가 준다. */
function RowSkeleton({ rows, height = 'h-8' }: { rows: number; height?: string }) {
  return (
    <div className="flex flex-col gap-3 py-1">
      {Array.from({ length: rows }).map((_, index) => (
        <Skeleton key={index} className={height} />
      ))}
    </div>
  );
}
