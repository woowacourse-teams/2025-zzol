import { ArrowUpRight, Trophy } from 'lucide-react';
import { useRoomDetail } from '@/api/queries';
import type { RoomPlayer } from '@/api/types';
import { ContextPanel } from '@/components/ui/ContextPanel';
import { ErrorState, Skeleton } from '@/components/ui/EmptyState';
import { KeyValue } from '@/components/ui/KeyValue';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Timestamp } from '@/components/ui/Timestamp';
import { formatPercent } from '@/lib/format';
import { miniGameLabel } from '@/lib/labels';
import { roomStatusBadge } from '@/pages/lookup/roomStatus';

/**
 * 방 한 건. "우리 방 결과가 이상해요" 문의에 답하는 자리다.
 *
 * <p>참여자 줄에서 <b>그 유저로 건너뛸 수 있다.</b> 예전에는 방 상세가 별도 화면이라
 * 참여자 이름을 보고 그 사람의 다른 기록을 보려면 유저 화면으로 가서 닉네임을 다시
 * 입력해야 했다. 문의는 대개 "이 방의 이 사람"으로 오므로 그 경로가 끊겨 있으면
 * 조사가 매번 검색부터 다시 시작된다.
 *
 * <p>게스트는 건너뛸 수 없다. 계정이 없어서 볼 기록 자체가 없다. 링크처럼 보이게 두고
 * 눌렀을 때 아무 일도 안 일어나는 것보다 처음부터 평범한 글자인 편이 낫다.
 */
export function RoomPanel({
  roomId,
  onClose,
  onPivotToUser,
}: {
  roomId: number | null;
  onClose: () => void;
  onPivotToUser: (userId: number) => void;
}) {
  const detail = useRoomDetail(roomId);
  const data = roomId === null ? undefined : detail.data;

  return (
    <ContextPanel
      open={roomId !== null}
      onClose={onClose}
      width="wide"
      title={data ? data.summary.joinCode : '방'}
      description={data ? `방 #${data.summary.id}` : undefined}
    >
      {roomId !== null && detail.isError && (
        <ErrorState message={(detail.error as Error).message} onRetry={() => detail.refetch()} />
      )}

      {roomId !== null && detail.isPending && (
        <div className="flex flex-col gap-3">
          <Skeleton className="h-20" />
          <Skeleton className="h-32" />
        </div>
      )}

      {data && (
        <div className="flex flex-col gap-6">
          <KeyValue
            items={[
              { label: '상태', value: roomStatusBadge(data.summary.status) },
              { label: '참여자', value: `${data.summary.playerCount}명` },
              { label: '생성', value: <Timestamp value={data.summary.createdAt} /> },
              {
                label: '종료',
                value: data.summary.finishedAt ? (
                  <Timestamp value={data.summary.finishedAt} />
                ) : null,
              },
            ]}
          />

          <Section title="룰렛 결과">
            {data.roulette ? (
              <div className="flex items-center gap-3">
                <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-attention-solid text-attention-icon-on-solid">
                  <Trophy className="size-4" aria-hidden />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-xl font-bold leading-none tracking-metric text-ink">
                    {data.roulette.winnerPlayerName}
                  </p>
                  <p className="mt-1 text-xs text-ink-muted">
                    당첨 확률 {formatPercent(data.roulette.winnerProbability / 100, 0)}
                  </p>
                </div>
              </div>
            ) : (
              <p className="text-xs text-ink-muted">
                룰렛까지 가지 않았습니다. 중간에 방이 끝났고 정상적인 경우입니다.
              </p>
            )}
          </Section>

          <Section title={`참여자 ${data.players.length}명`}>
            {data.players.length === 0 ? (
              <p className="text-xs text-ink-muted">참여자 기록이 없습니다.</p>
            ) : (
              <ul className="divide-y divide-border-default">
                {data.players.map((player) => (
                  <PlayerRow key={player.id} player={player} onPivot={onPivotToUser} />
                ))}
              </ul>
            )}
          </Section>

          <Section title="미니게임 결과">
            {data.miniGameResults.length === 0 ? (
              <p className="text-xs text-ink-muted">
                게임을 시작하지 않았거나 끝내지 않았습니다.
              </p>
            ) : (
              <div className="flex flex-col gap-4">
                {groupByGame(data.miniGameResults).map(([gameType, results]) => (
                  <div key={gameType}>
                    <p className="mb-1.5 text-xs font-semibold text-ink-secondary">
                      {miniGameLabel(gameType)}
                    </p>
                    <ol className="divide-y divide-border-default rounded-md border border-border-default">
                      {results.map((result) => (
                        <li
                          key={`${result.playerId}-${result.createdAt}`}
                          className="flex items-center gap-3 px-3 py-2 text-sm"
                        >
                          <span className="w-6 text-center font-mono text-xs text-ink-muted">
                            {result.rank}
                          </span>
                          <span className="flex-1 truncate">{result.playerName}</span>
                          <span className="tabular-nums text-ink-secondary">
                            {result.score ?? '-'}
                          </span>
                        </li>
                      ))}
                    </ol>
                  </div>
                ))}
              </div>
            )}
          </Section>
        </div>
      )}
    </ContextPanel>
  );
}

function PlayerRow({
  player,
  onPivot,
}: {
  player: RoomPlayer;
  onPivot: (userId: number) => void;
}) {
  const body = (
    <>
      <span className="min-w-0 flex-1 truncate font-medium text-ink">{player.playerName}</span>
      {player.playerType === 'HOST' && <StatusBadge tone="neutral">방장</StatusBadge>}
      {player.guest ? (
        <StatusBadge tone="muted">게스트</StatusBadge>
      ) : (
        <span className="shrink-0 font-mono text-xs text-ink-muted">{player.userCode}</span>
      )}
    </>
  );

  if (player.guest || player.userId === null) {
    return <li className="flex items-center gap-2.5 py-2 text-sm">{body}</li>;
  }

  return (
    <li>
      <button
        type="button"
        onClick={() => onPivot(player.userId as number)}
        className="flex w-full items-center gap-2.5 rounded-sm py-2 text-left text-sm transition-colors hover:bg-subtle"
      >
        {body}
        <ArrowUpRight className="size-3.5 shrink-0 text-ink-muted" aria-hidden />
      </button>
    </li>
  );
}

/**
 * 패널 안의 구획.
 *
 * <p>카드를 겹쳐 쓰지 않는다. 패널이 이미 떠 있는 판이라 그 안에 또 흰 카드를 넣으면
 * 층이 셋이 되어 어느 것이 바깥인지 흐려진다. 제목과 여백만으로 나눈다.
 */
function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h3 className="mb-2 text-xs font-semibold text-ink-secondary">{title}</h3>
      {children}
    </section>
  );
}

/**
 * 게임별로 묶고 순위로 정렬한다.
 *
 * <p>평평하게 나열하면 한 방에서 여러 게임을 했을 때 매 행마다 어느 게임의 결과인지를
 * 확인해야 한다.
 */
function groupByGame<T extends { miniGameType: string; rank: number }>(results: T[]) {
  const grouped = new Map<string, T[]>();
  for (const result of results) {
    const bucket = grouped.get(result.miniGameType) ?? [];
    bucket.push(result);
    grouped.set(result.miniGameType, bucket);
  }
  return [...grouped.entries()].map(
    ([gameType, rows]) => [gameType, rows.slice().sort((a, b) => a.rank - b.rank)] as const,
  );
}
