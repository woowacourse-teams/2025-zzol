import { ThumbsDown, ThumbsUp } from 'lucide-react';
import { useRef, useState } from 'react';
import { useZzolBotFeedback, useZzolBotSessions } from '@/api/queries';
import type { ZzolBotFeedback } from '@/api/types';
import { askZzolBot } from '@/api/zzolbotStream';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { EmptyState, ErrorState, Skeleton } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/Field';
import { cn } from '@/lib/cn';

type Turn =
  | { kind: 'question'; text: string }
  | { kind: 'answer'; text: string; sessionId: number | null }
  | { kind: 'progress'; text: string }
  | { kind: 'error'; text: string };

/**
 * ZzolBot 에게 묻는 화면.
 *
 * <p>답이 오기까지 수십 초가 걸린다. 그동안 어떤 도구를 부르고 있는지를 그대로 흘린다.
 * 스피너만 돌리면 사용자는 멈춘 것인지 도는 것인지 모르고, 실제로 느린 도구가 무엇인지도
 * 알 수 없다. 답이 도착하면 진행 줄은 지운다. 기록으로 남길 것은 질문과 답이다.
 */
export function ZzolBotChatPanel() {
  const [question, setQuestion] = useState('');
  const [turns, setTurns] = useState<Turn[]>([]);
  const [asking, setAsking] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const sessions = useZzolBotSessions();
  const feedback = useZzolBotFeedback();

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = question.trim();
    if (trimmed === '' || asking) {
      return;
    }

    setTurns((prev) => [...prev, { kind: 'question', text: trimmed }]);
    setQuestion('');
    setAsking(true);

    const controller = new AbortController();
    abortRef.current = controller;
    let sessionId: number | null = null;

    try {
      await askZzolBot(
        trimmed,
        {
          onProgress: (tool) =>
            setTurns((prev) => [...prev, { kind: 'progress', text: tool }]),
          onSessionId: (id) => {
            sessionId = id;
          },
          onResult: (answer) =>
            // 진행 줄을 걷어내고 답으로 바꾼다. 남겨 두면 다음 질문의 진행 줄과 섞인다.
            setTurns((prev) => [
              ...prev.filter((turn) => turn.kind !== 'progress'),
              { kind: 'answer', text: answer, sessionId },
            ]),
        },
        controller.signal,
      );
      sessions.refetch();
    } catch (error) {
      setTurns((prev) => [
        ...prev.filter((turn) => turn.kind !== 'progress'),
        { kind: 'error', text: (error as Error).message },
      ]);
    } finally {
      setAsking(false);
      abortRef.current = null;
    }
  };

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
      <Card className="flex flex-col">
        <CardHeader
          title="질문"
          description="운영 DB 를 읽어 답합니다. 쓰기는 하지 않습니다."
          actions={
            asking && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => abortRef.current?.abort()}
              >
                중단
              </Button>
            )
          }
        />

        {/* 대화가 남는 높이를 가져간다.
          *
          * 카드를 세로 플렉스로 두지 않았을 때는 늘어난 높이가 <b>입력칸 아래</b>에
          * 남았다. 옆 카드가 여덟 줄이라 이 카드도 그 높이로 늘어나는데, 정작 입력칸은
          * 화면 가운데쯤에 떠 있고 그 아래 100px 넘게 비어 있었다. 채팅에서 입력칸은
          * 바닥에 있어야 손이 거기로 간다.
          *
          * 대화가 길어지면 이 영역만 스크롤한다. 카드 전체가 늘어나면 입력칸이 화면
          * 밖으로 밀려 한 줄 칠 때마다 스크롤을 내려야 한다. */}
        <CardBody className="flex min-h-[22rem] flex-1 flex-col gap-2.5 overflow-y-auto">
          {turns.length === 0 ? (
            <EmptyState
              title="아직 주고받은 말이 없습니다"
              description="예: 어제 방이 몇 개 만들어졌고 그중 몇 개가 끝까지 갔어?"
            />
          ) : (
            turns.map((turn, index) => <TurnRow key={index} turn={turn} onFeedback={feedback.mutate} />)
          )}
        </CardBody>

        <form onSubmit={submit} className="flex shrink-0 gap-2 border-t border-border-default p-5">
          <Input
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="무엇이든 물어보세요"
            aria-label="질문"
            disabled={asking}
          />
          <Button type="submit" disabled={asking || question.trim() === ''}>
            {asking ? '묻는 중' : '보내기'}
          </Button>
        </form>
      </Card>

      <Card className="h-fit">
        <CardHeader
          title="최근 질문"
          description="좋았는지 아닌지를 남기면 프롬프트를 고칠 근거가 됩니다."
        />
        {sessions.isPending ? (
          <CardBody className="flex flex-col gap-3">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} className="h-10" />
            ))}
          </CardBody>
        ) : sessions.isError ? (
          <ErrorState
            message={(sessions.error as Error).message}
            onRetry={() => sessions.refetch()}
          />
        ) : sessions.data?.length === 0 ? (
          <EmptyState title="기록이 없습니다" />
        ) : (
          <ul className="divide-y divide-border-default">
            {sessions.data?.map((session) => (
              <li key={session.id} className="px-5 py-3">
                <p className="line-clamp-2 text-xs text-ink">{session.question}</p>
                <p className="mt-1 flex items-center gap-2 text-2xs text-ink-muted">
                  {session.createdAt}
                  {/* 아래 평가 버튼과 같은 글리프를 쓴다. 한때 여기만 컬러 이모지였는데,
                    * 회색과 코랄뿐인 화면에서 그 두 글자만 색이 튀었고 버튼과 같은 뜻인
                    * 것도 바로 안 읽혔다. */}
                  {session.feedback && (
                    <span className="inline-flex items-center gap-1">
                      {session.feedback === 'GOOD' ? (
                        <ThumbsUp className="size-3" aria-hidden />
                      ) : (
                        <ThumbsDown className="size-3" aria-hidden />
                      )}
                      {session.feedback === 'GOOD' ? '좋았음' : '아쉬움'}
                    </span>
                  )}
                </p>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

function TurnRow({
  turn,
  onFeedback,
}: {
  turn: Turn;
  onFeedback: (input: { id: number; feedback: ZzolBotFeedback }) => void;
}) {
  if (turn.kind === 'question') {
    return (
      <p className="self-end rounded-lg rounded-br-sm bg-subtle px-3 py-2 text-sm text-ink">
        {turn.text}
      </p>
    );
  }

  if (turn.kind === 'progress') {
    return (
      <p className="flex items-center gap-2 text-xs text-ink-muted">
        {/* 점 하나가 뛴다. 스피너를 여러 개 두면 진행 줄이 쌓일 때 화면이 어지럽다. */}
        <span className="size-1.5 animate-pulse rounded-full bg-attention-mark" aria-hidden />
        {turn.text}
      </p>
    );
  }

  if (turn.kind === 'error') {
    return (
      <p className="rounded-lg bg-attention-bg px-3 py-2 text-xs text-attention">
        답을 받지 못했습니다. {turn.text}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-1.5">
      {/* 답에 줄바꿈과 표가 섞여 온다. Markdown 을 렌더링하지 않고 원문 그대로 둔다.
       * 렌더러를 붙이면 모델이 만든 마크업을 화면이 해석하게 되고, 무엇이 모델의 말이고
       * 무엇이 화면의 해석인지가 흐려진다. */}
      <pre className="whitespace-pre-wrap break-words rounded-lg border border-border-default bg-surface px-3 py-2 font-sans text-sm leading-relaxed text-ink">
        {turn.text}
      </pre>
      {turn.sessionId !== null && (
        <div className="flex gap-1.5">
          <FeedbackButton
            label="좋았음"
            icon={ThumbsUp}
            onClick={() => onFeedback({ id: turn.sessionId as number, feedback: 'GOOD' })}
          />
          <FeedbackButton
            label="아쉬움"
            icon={ThumbsDown}
            onClick={() => onFeedback({ id: turn.sessionId as number, feedback: 'BAD' })}
          />
        </div>
      )}
    </div>
  );
}

function FeedbackButton({
  label,
  icon: Icon,
  onClick,
}: {
  label: string;
  icon: typeof ThumbsUp;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'inline-flex items-center gap-1 rounded-md border border-border-default px-2 py-1',
        'text-2xs text-ink-secondary transition-colors hover:border-border-strong hover:text-ink',
      )}
    >
      <Icon className="size-3.5" aria-hidden />
      {label}
    </button>
  );
}
