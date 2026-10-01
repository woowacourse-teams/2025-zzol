import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useUserSearch, useUserStats } from '@/api/queries';
import type { UserRow, UserStats } from '@/api/types';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { SearchInput, Select } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pagination } from '@/components/ui/Pagination';
import { Timestamp } from '@/components/ui/Timestamp';
import { Loaded } from '@/components/ui/Loaded';
import { Skeleton } from '@/components/ui/EmptyState';
import { ShareNote } from '@/components/ui/ShareNote';
import { DataTable } from '@/components/DataTable';
import { Histogram } from '@/components/charts/Histogram';
import { DailyChart } from '@/components/charts/DailyChart';
import { ProviderShare } from '@/components/ProviderShare';
import { readId } from '@/pages/RoomsPage';
import { UserPanel } from '@/pages/lookup/UserPanel';
import { miniGameLabel } from '@/lib/labels';
import { useDebounced } from '@/lib/useDebounced';

/**
 * 가입은 선이다.
 *
 * <p>견줄 상대가 없는 한 계열이라 읽을 것이 흐름뿐이다. 막대 서른 개를 세우면 그 흐름이
 * 톱니로 흩어지고, 하루 한두 명인 값이라 막대 하나하나는 어차피 눈금 한 칸이다.
 * 계열이 둘인 화면(접수와 처리, 성공과 실패)은 같은 이유로 반대편에 있다.
 */
const SIGNUP_SERIES = [
  { key: 'count', name: '가입', color: 'var(--chart-1)', shape: 'line' as const },
];

/** 가입 추이 기간. 서버가 180일까지 받는다. */
const RANGES = [14, 30, 90] as const;

/**
 * 유저 조회.
 *
 * <p>두 가지 일을 한 화면에서 한다. 아래는 문의가 들어온 사람을 찾는 자리고, 위는
 * <b>회원 전체가 어떤 모양인가</b>를 보는 자리다. 목록만 있으면 "이 사람"에만 답할 수
 * 있는데, 운영에서 실제로 묻는 것은 그 위에 하나 더 있다. 한 번 해 보고 안 돌아온 사람이
 * 절반이면 목록을 아무리 넘겨도 그 사실은 보이지 않는다.
 *
 * <p>방에서 건너온 경우 주소에 {@code from=room:12} 가 붙는다. 조사는 방에서 사람으로
 * 갔다가 다시 방으로 오는 왕복이라, 되돌아갈 길이 없으면 목록에서 그 방을 다시 찾아야 한다.
 */
export function UsersPage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  const [input, setInput] = useState(() => params.get('q') ?? '');
  const [page, setPage] = useState(0);
  const [days, setDays] = useState<number>(30);

  const keyword = useDebounced(input, 300);
  const users = useUserSearch(keyword, page);
  const stats = useUserStats(days);

  const openId = readId(params.get('open'), 'user');
  const fromRoomId = readId(params.get('from'), 'room');

  const open = (userId: number) => {
    const updated = new URLSearchParams(params);
    updated.set('open', `user:${userId}`);
    // 목록에서 직접 연 것이라 방에서 왔다는 자취는 지운다. 남겨 두면 엉뚱한 방으로
    // 돌아가는 링크가 뜬다.
    updated.delete('from');
    setParams(updated, { replace: true });
  };

  const close = () => {
    const updated = new URLSearchParams(params);
    updated.delete('open');
    updated.delete('from');
    setParams(updated, { replace: true });
  };

  const columns = useMemo<ColumnDef<UserRow, unknown>[]>(
    () => [
      {
        accessorKey: 'userCode',
        header: '유저코드',
        meta: { width: '8rem' },
        cell: (c) => <span className="font-mono font-medium">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'nickname',
        header: '닉네임',
        cell: (c) =>
          c.getValue() ? String(c.getValue()) : <span className="text-ink-muted">(없음)</span>,
      },
      // 활동량이 목록에 실린다. 문의 대응에서 먼저 묻는 것은 "얼마나, 무엇을, 언제까지
      // 했나"인데 그동안은 행을 하나씩 열어야 알 수 있었다.
      {
        accessorKey: 'playCount',
        header: '플레이',
        meta: { width: '6rem', align: 'right' },
        // 0은 흐리게 둔다. 가입만 하고 만 회원이 많아 또렷하면 0만 줄줄이 눈에 걸린다.
        cell: (c) => {
          const value = Number(c.getValue());
          return <span className={value === 0 ? 'text-ink-muted' : undefined}>{value}</span>;
        },
      },
      {
        accessorKey: 'topGame',
        header: '주로 하는 게임',
        meta: { width: '10rem' },
        // 한글 이름은 화면이 붙인다. 모르는 값이 와도 enum 이름 그대로 찍혀 사라지지 않는다.
        cell: (c) =>
          c.getValue() ? (
            miniGameLabel(String(c.getValue()))
          ) : (
            <span className="text-ink-muted">-</span>
          ),
      },
      {
        accessorKey: 'lastPlayedAt',
        header: '최근 플레이',
        meta: { width: '9rem' },
        // 여기서 묻는 것은 "언제였나"가 아니라 "얼마나 됐나"다. 석 달 전이면 그 자체로
        // 답이 되고, 정확한 시각은 툴팁에 남는다.
        cell: (c) => <Timestamp value={c.getValue() as string | null} relativeOnly />,
      },
      {
        accessorKey: 'createdAt',
        header: '가입',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} />,
      },
    ],
    [],
  );

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="유저"
        description="닉네임은 부분 일치, 유저코드는 정확히 일치입니다."
        actions={
          <SearchInput
            value={input}
            onChange={(event) => {
              setInput(event.target.value);
              setPage(0);
            }}
            placeholder="닉네임 또는 유저코드"
            className="w-56"
          />
        }
      />

      {/* 가입 추이와 제공자를 한 줄에 둔다. 둘 다 "어떻게 들어왔나"에 대한 답이라,
       * 가입이 튄 날 어느 제공자가 늘었는지를 같은 눈높이에서 본다. */}
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <Card className="flex flex-col xl:col-span-2">
          <CardHeader
            title="가입 추이"
            description="가입이 없는 날도 빈칸으로 남깁니다."
            actions={
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
            }
          />
          {/* 옆의 도넛 카드가 더 길어서 이 카드가 늘어난다. 늘어난 자리를 그림이 쓰게
           * 둔다. 고정 높이로 두면 카드 바닥에 200px 짜리 빈 공간이 남는다. */}
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-48" />}>
              {(data) => <DailyChart data={data.signups} series={SIGNUP_SERIES} height="100%" />}
            </Loaded>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="소셜 제공자" description="연결 기준입니다. 회원 수와 다릅니다." />
          <CardBody>
            <Loaded query={stats}>
              {(data) => (
                <ProviderShare stats={{ userCount: data.userCount, providers: data.providers }} />
              )}
            </Loaded>
          </CardBody>
        </Card>
      </div>

      {/* 참여도와 잔존. 둘 다 회원 전체가 대상이고 기간을 타지 않는다. 기간을 걸면 그
       * 기간에 활동한 사람만 남아, 빠져나간 사람을 묻는 그래프에서 빠져나간 사람이
       * 사라진다. 그 사실을 카드 설명에 적어 둔다. */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card className="flex flex-col">
          <CardHeader
            title="참여도"
            description="끝낸 판 수로 나눈 회원 수입니다. 기간과 무관한 전체 회원입니다."
            // 제목 옆 숫자는 값이 있을 때만 붙인다. Loaded 를 쓰지 않는 자리다. 실패가
            // 조용히 사라지는 것을 막으려는 규칙인데, 같은 카드의 본문이 이미 실패를
            // 그리고 있어 여기까지 오류 상자를 하나 더 세우면 카드에 경고가 둘이 된다.
            actions={stats.data && <PlayedShare stats={stats.data} />}
          />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <Histogram
                  data={data.playBuckets}
                  emptyTitle="회원이 없습니다"
                  emptyDescription="가입이 있어야 분포가 그려집니다."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>

        <Card className="flex flex-col">
          <CardHeader
            title="마지막 참여"
            description="마지막으로 방에 들어온 뒤 지난 날입니다."
            actions={stats.data && <ActiveShare stats={stats.data} />}
          />
          <CardBody className="flex-1 pb-4">
            <Loaded query={stats} skeleton={<Skeleton className="h-40" />}>
              {(data) => (
                <Histogram
                  data={data.activityBuckets}
                  emptyTitle="회원이 없습니다"
                  emptyDescription="가입이 있어야 분포가 그려집니다."
                />
              )}
            </Loaded>
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader title="유저 목록" description="행을 누르면 활동 기록이 옆에서 열립니다." />
        <DataTable
          columns={columns}
          data={users.data?.content ?? []}
          loading={users.isPending}
          error={users.error}
          onRetry={() => users.refetch()}
          onRowClick={(user) => open(user.id)}
          isRowSelected={(user) => user.id === openId}
          emptyTitle={keyword ? `'${keyword}' 유저를 찾지 못했습니다` : '유저가 없습니다'}
          emptyDescription={keyword ? '닉네임 일부만 넣어도 찾습니다.' : undefined}
        />
        {users.data && (
          <Pagination
            page={users.data.page}
            totalPages={users.data.totalPages}
            totalElements={users.data.totalElements}
            onChange={setPage}
          />
        )}
      </Card>

      <UserPanel
        userId={openId}
        onBack={fromRoomId === null ? null : () => navigate(`/rooms?open=room:${fromRoomId}`)}
        onClose={close}
      />
    </div>
  );
}

/**
 * 한 판이라도 끝낸 회원의 비율.
 *
 * <p>막대 다섯 개를 눈으로 더해야 나오는 값이라 제목 옆에 적어 둔다. 분포는 모양을
 * 말하고 이 숫자는 결론을 말한다. 더하는 일은 서버가 한다.
 */
function PlayedShare({ stats }: { stats: UserStats }) {
  return (
    <ShareNote value={stats.playedUserCount} total={stats.userCount} suffix="명이 한 판 이상" />
  );
}

/** 최근 7일 안에 방에 들어온 회원의 비율. 주간 활동 회원이다. */
function ActiveShare({ stats }: { stats: UserStats }) {
  return <ShareNote value={stats.activeUserCount} total={stats.userCount} suffix="명이 최근 7일" />;
}
