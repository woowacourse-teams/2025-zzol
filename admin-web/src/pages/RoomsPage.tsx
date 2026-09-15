import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useRoomSearch, useRoomStats } from '@/api/queries';
import type { RoomState, RoomStats, RoomSummary } from '@/api/types';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { SearchInput, Select } from '@/components/ui/Field';
import { Loaded } from '@/components/ui/Loaded';
import { Skeleton } from '@/components/ui/EmptyState';
import { ShareNote } from '@/components/ui/ShareNote';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pagination } from '@/components/ui/Pagination';
import { Timestamp } from '@/components/ui/Timestamp';
import { DataTable } from '@/components/DataTable';
import { DistributionBars } from '@/components/charts/DistributionBars';
import { Histogram } from '@/components/charts/Histogram';
import { RoomPanel } from '@/pages/lookup/RoomPanel';
import { roomStatusBadge } from '@/pages/lookup/roomStatus';
import { formatNumber } from '@/lib/format';
import { toGameRowsWithDropOff } from '@/lib/gameStats';
import { roomStateLabel } from '@/lib/labels';
import { useDebounced } from '@/lib/useDebounced';

/** 상단 그래프 기간. 기간 안의 방을 한 줄씩 읽으므로 서버가 90일에서 끊는다. */
const RANGES = [7, 30, 90] as const;

/**
 * 방 조회. "우리 방 결과가 이상해요" 문의에 답하는 화면이다.
 *
 * <p>한때 유저 조회와 한 화면에 있었다. 단서 하나로 양쪽을 동시에 찾자는 것이었는데,
 * 표 두 개가 세로로 쌓이면서 <b>어느 표를 보고 있는지가 흐려졌고</b> 화면이 3800px 이
 * 됐다. 찾는 대상이 방인지 사람인지는 문의를 받은 시점에 이미 정해져 있다.
 *
 * <p>상세는 라우트가 아니라 패널이다. 화면이 통째로 바뀌면 돌아왔을 때 검색어와 페이지가
 * 초기화되는데, 조사는 한 번에 끝나는 일이 아니라 목록과 상세를 오가는 왕복이다.
 */
export function RoomsPage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  // 주소의 q 는 초기값으로만 읽는다. 신고 패널의 방 링크가 이 값을 들고 온다.
  // 계속 따라가면 지우고 다시 치는 동안 주소가 입력을 덮어쓴다.
  const [input, setInput] = useState(() => params.get('q') ?? '');
  const [page, setPage] = useState(0);
  const [days, setDays] = useState<number>(30);

  // 타이핑마다 서버를 때리면 다섯 글자 코드에 다섯 번 조회한다.
  const joinCode = useDebounced(input, 300);
  const rooms = useRoomSearch(joinCode, page);
  const stats = useRoomStats(days);

  const openId = readId(params.get('open'), 'room');

  const open = (roomId: number) => {
    const updated = new URLSearchParams(params);
    updated.set('open', `room:${roomId}`);
    // 갈음한다. 패널을 여닫은 횟수만큼 뒤로가기를 눌러야 목록을 벗어나면 뒤로가기가
    // 쓸모없어진다.
    setParams(updated, { replace: true });
  };

  const close = () => {
    const updated = new URLSearchParams(params);
    updated.delete('open');
    setParams(updated, { replace: true });
  };

  const columns = useMemo<ColumnDef<RoomSummary, unknown>[]>(
    () => [
      {
        accessorKey: 'joinCode',
        header: 'joinCode',
        meta: { width: '8rem' },
        cell: (c) => <span className="font-mono font-medium">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'status',
        header: '상태',
        meta: { width: '9rem' },
        cell: (c) => roomStatusBadge(c.getValue() as RoomState),
      },
      { accessorKey: 'playerCount', header: '참여자', meta: { width: '6rem', align: 'right' } },
      {
        accessorKey: 'createdAt',
        header: '생성',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} />,
      },
      {
        accessorKey: 'finishedAt',
        header: '종료',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string | null} absoluteOnly />,
      },
    ],
    [],
  );

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="방"
        description="joinCode 로 방을 찾아 참여자, 게임 결과, 룰렛 결과를 확인합니다. 같은 코드가 재사용되므로 결과가 여러 개일 수 있습니다."
        actions={
          <SearchInput
            value={input}
            onChange={(event) => {
              setInput(event.target.value);
              setPage(0);
            }}
            placeholder="joinCode (비우면 전체)"
            className="w-56"
          />
        }
      />

      {/* 기간 선택은 네 카드가 함께 쓴다. 카드마다 두면 같은 화면에 서로 다른 기간의
       * 그래프가 나란히 서서, 인원 분포와 게임 비중이 같은 방들을 말하는지 아닌지를
       * 매번 확인해야 한다. 한 줄 위에 하나만 둔다. */}
      <div className="flex items-center justify-between gap-3">
        <p className="text-xs text-ink-muted">
          {stats.data
            ? `최근 ${days}일에 만들어진 방 ${formatNumber(stats.data.roomCount)}개를 봅니다.`
            : `최근 ${days}일에 만들어진 방을 봅니다.`}
        </p>
        <Select
          value={String(days)}
          onChange={(event) => setDays(Number(event.target.value))}
          className="w-24"
        >
          {RANGES.map((range) => (
            <option key={range} value={range}>
              최근 {range}일
            </option>
          ))}
        </Select>
      </div>

      {/* 네 카드를 2x2 로 세운다. 넷을 한 줄에 펴면 막대 하나가 100px 이 안 되고,
        * 세로로 쌓으면 인원과 소요 시간을 같이 보려고 스크롤을 오간다. 둘씩 두 줄이면
        * 네 카드가 한 화면에 들어오면서 막대가 카드 폭을 쓴다.
        *
        * 순서는 판, 방, 방, 방이다. 게임별 플레이가 먼저고 나머지 셋이 방을 인원,
        * 시간, 멈춘 자리 순으로 본다. */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* 이탈을 이 카드 안에 적는다. 한때 이탈률 순으로 다시 세운 카드를 옆에 따로
          * 뒀는데, 같은 게임 목록이 다른 순서로 두 번 서니 한 게임을 두 카드에서 찾아
          * 맞춰 보게 됐다. 줄마다 "10판 중 4판 이탈 40%" 가 붙으면 순서는 판 수 하나로
          * 족하다. 기간 전체 이탈률은 제목 옆에 남긴다 - 줄을 눈으로 더해야 나오는
          * 수이고, 이 수가 평소보다 크면 게임 하나가 아니라 서버 쪽을 봐야 한다. */}
        <Card className="flex flex-col">
          <CardHeader
            title="게임별 플레이"
            description="시작한 판을 셉니다. 막대의 옅은 꼬리와 오른쪽 숫자가 이탈입니다."
            actions={stats.data && <DropOffShare stats={stats.data} />}
          />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <DistributionBars
                  ranked
                  data={toGameRowsWithDropOff(data.games)}
                  emptyTitle="시작된 게임이 없습니다"
                  emptyDescription="게임이 시작돼야 집계됩니다."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader
            title="방 인원"
            description="방을 만든 사람을 포함한 참여자 수입니다."
            actions={stats.data && <SoloShare stats={stats.data} />}
          />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <Histogram
                  data={data.playerBuckets}
                  emptyTitle="방이 없습니다"
                  emptyDescription="기간을 늘려 보세요."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader title="소요 시간" description="끝난 방만 셉니다. 진행 중인 방은 빠집니다." />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <Histogram
                  data={data.durationBuckets}
                  emptyTitle="끝난 방이 없습니다"
                  emptyDescription="방이 끝나야 소요 시간이 남습니다."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader
            title="진행 단계"
            description="방이 멈춘 자리입니다. 끝까지 간 방만 완주입니다."
            actions={stats.data && <DoneShare stats={stats.data} />}
          />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <DistributionBars
                  data={data.statuses.map((slice) => ({
                    label: roomStateLabel(slice.status),
                    count: slice.count,
                  }))}
                  emptyTitle="방이 없습니다"
                  emptyDescription="기간을 늘려 보세요."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader title="방 목록" description="행을 누르면 그 방의 기록이 옆에서 열립니다." />
        <DataTable
          columns={columns}
          data={rooms.data?.content ?? []}
          loading={rooms.isPending}
          error={rooms.error}
          onRetry={() => rooms.refetch()}
          onRowClick={(room) => open(room.id)}
          isRowSelected={(room) => room.id === openId}
          emptyTitle={joinCode ? `'${joinCode}' 방을 찾지 못했습니다` : '방이 없습니다'}
          emptyDescription={joinCode ? '코드를 다시 확인해주세요.' : undefined}
        />
        {rooms.data && (
          <Pagination
            page={rooms.data.page}
            totalPages={rooms.data.totalPages}
            totalElements={rooms.data.totalElements}
            onChange={setPage}
          />
        )}
      </Card>

      {/* 참여자에서 그 사람으로 건너뛴다. 화면은 바뀌지만 패널은 열린 채로 이어지고,
       * 유저 쪽에서 이 방으로 돌아오는 길이 남는다. 문의는 대개 "이 방의 이 사람"으로
       * 오므로 그 경로가 끊겨 있으면 조사가 매번 검색부터 다시 시작된다. */}
      <RoomPanel
        roomId={openId}
        onClose={close}
        onPivotToUser={(userId) => navigate(`/users?open=user:${userId}&from=room:${openId}`)}
      />
    </div>
  );
}

/**
 * 참여자가 방장 하나뿐인 방의 비율.
 *
 * <p>막대를 눈으로 더해야 나오는 값이라 제목 옆에 적는다. 이 값이 크면 방은 만들어지는데
 * 초대가 안 닿고 있다는 뜻이고, 그건 게임이나 룰렛과 무관한 문제다.
 */
function SoloShare({ stats }: { stats: RoomStats }) {
  return <ShareNote value={stats.soloRoomCount} total={stats.roomCount} suffix="개가 혼자" />;
}

/**
 * 기간 전체의 이탈 비율.
 *
 * <p>줄마다 비율이 적혀 있어도 "그래서 전체로는 얼마나 깨지나"는 눈으로 더해야 나온다.
 * 이 수가 평소보다 크면 게임 하나가 아니라 서버 쪽을 봐야 한다는 뜻이다.
 */
function DropOffShare({ stats }: { stats: RoomStats }) {
  const started = stats.games.reduce((sum, stat) => sum + stat.started, 0);
  const dropped = stats.games.reduce((sum, stat) => sum + (stat.started - stat.finished), 0);
  return <ShareNote value={dropped} total={started} suffix="판이 이탈" />;
}

/** 끝까지 간 방의 비율. */
function DoneShare({ stats }: { stats: RoomStats }) {
  const done = stats.statuses.find((slice) => slice.status === 'DONE')?.count ?? 0;
  return <ShareNote value={done} total={stats.roomCount} suffix="개가 완주" />;
}

/** `room:12` 에서 12를 꺼낸다. 종류가 다르거나 숫자가 아니면 열지 않는다. */
export function readId(raw: string | null, kind: string): number | null {
  const [prefix, value] = (raw ?? '').split(':');
  const id = Number(value);
  return prefix === kind && Number.isFinite(id) ? id : null;
}
