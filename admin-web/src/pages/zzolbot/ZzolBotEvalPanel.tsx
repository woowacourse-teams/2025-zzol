import type { ColumnDef } from '@tanstack/react-table';
import { lazy, Suspense, useMemo, useState } from 'react';
import {
  useDeleteEvalScenario,
  useEvalRunDetail,
  useEvalRuns,
  useEvalScenarios,
  useStartEvalRun,
} from '@/api/queries';
import type { EvalResult, EvalRun, EvalScenario } from '@/api/types';
import { ApiError } from '@/api/client';
import { DataTable } from '@/components/DataTable';
import { evalKindLabel, evalSourceLabel } from '@/lib/labels';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { ErrorState, Skeleton } from '@/components/ui/EmptyState';
import { Input, Select } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatPercent } from '@/lib/format';

/** recharts 를 지연 로드한다. ZzolBot 화면은 대화 탭으로 먼저 들어오는 일이 많다. */
const EvalPassRateChart = lazy(() =>
  import('@/components/EvalPassRateChart').then((module) => ({
    default: module.EvalPassRateChart,
  })),
);

/**
 * 골든셋 평가.
 *
 * <p>ZzolBot 의 프롬프트나 모델을 바꿨을 때 좋아졌는지 나빠졌는지를 숫자로 본다.
 * 이 화면이 없으면 "느낌상 나아진 것 같다"로 배포하게 된다.
 */
export function ZzolBotEvalPanel() {
  const runs = useEvalRuns();
  const scenarios = useEvalScenarios();
  const [openRunId, setOpenRunId] = useState<number | null>(null);
  const detail = useEvalRunDetail(openRunId);

  const runColumns = useMemo<ColumnDef<EvalRun, unknown>[]>(
    () => [
      { accessorKey: 'label', header: '라벨' },
      {
        accessorKey: 'model',
        header: '모델',
        meta: { width: '12rem' },
        cell: (c) => <span className="font-mono text-xs">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'status',
        header: '상태',
        meta: { width: '8rem' },
        cell: (c) =>
          c.getValue() === 'RUNNING' ? (
            <StatusBadge tone="attention">실행 중</StatusBadge>
          ) : (
            <StatusBadge tone="muted">{String(c.getValue())}</StatusBadge>
          ),
      },
      {
        id: 'pass',
        header: '통과',
        meta: { width: '9rem', align: 'right' },
        cell: (c) => {
          const row = c.row.original;
          return (
            <span className="tabular-nums">
              <span className="font-medium text-ink">
                {row.passCount}/{row.scenarioCount}
              </span>
              <span className="ml-1.5 text-xs text-ink-muted">
                {row.scenarioCount === 0
                  ? '-'
                  : formatPercent(row.passCount / row.scenarioCount, 0)}
              </span>
            </span>
          );
        },
      },
      {
        accessorKey: 'startedAt',
        header: '시작',
        meta: { width: '9rem' },
        cell: (c) => (
          <span className="tabular-nums text-ink-secondary">{String(c.getValue())}</span>
        ),
      },
    ],
    [],
  );

  const finishedRuns = (runs.data ?? []).filter(
    (run) => run.finishedAt !== null && run.scenarioCount > 0,
  );

  return (
    <div className="flex flex-col gap-4">
      {/* 표보다 위에 둔다. 이 화면에 들어오는 이유가 "좋아졌나"이고, 그 답이 여기 있다.
       * 점이 하나뿐이면 선이 안 그려지므로 둘 이상일 때만 낸다. */}
      {finishedRuns.length >= 2 && (
        <Card>
          <CardHeader
            title="통과율 추이"
            description="끝난 실행만. 왼쪽이 과거입니다. 세로축은 0~100%로 고정해 작은 차이가 급등락으로 보이지 않게 했습니다."
          />
          <CardBody>
            <Suspense fallback={<Skeleton className="h-[200px]" />}>
              <EvalPassRateChart runs={finishedRuns} />
            </Suspense>
          </CardBody>
        </Card>
      )}

      <Card>
        <CardHeader
          title="평가 실행"
          description="최근 20건. 줄을 누르면 시나리오별 결과가 펼쳐집니다."
          actions={<StartRunForm />}
        />
        {runs.isError ? (
          <ErrorState message={(runs.error as Error).message} onRetry={() => runs.refetch()} />
        ) : (
          <DataTable
            columns={runColumns}
            data={runs.data ?? []}
            loading={runs.isPending}
            onRowClick={(row) => setOpenRunId(row.id === openRunId ? null : row.id)}
            emptyTitle="실행 기록이 없습니다"
            emptyDescription="라벨을 적고 실행하면 골든셋 전체를 한 번 돕니다."
          />
        )}
      </Card>

      {openRunId !== null && (
        <Card>
          <CardHeader
            title={`실행 #${openRunId} 결과`}
            description={detail.data ? `${detail.data.results.length}개 시나리오` : '불러오는 중'}
          />
          {detail.isError ? (
            <ErrorState
              message={(detail.error as Error).message}
              onRetry={() => detail.refetch()}
            />
          ) : (
            <DataTable
              columns={RESULT_COLUMNS}
              data={detail.data?.results ?? []}
              loading={detail.isPending}
              emptyTitle="결과가 없습니다"
            />
          )}
        </Card>
      )}

      <ScenarioCard scenarios={scenarios} />
    </div>
  );
}

const RESULT_COLUMNS: ColumnDef<EvalResult, unknown>[] = [
  {
    accessorKey: 'scenarioId',
    header: '시나리오',
    meta: { width: '7rem' },
    cell: (c) => <span className="font-mono text-xs">#{String(c.getValue())}</span>,
  },
  {
    accessorKey: 'verdict',
    header: '판정',
    meta: { width: '7rem' },
    cell: (c) =>
      c.getValue() === 'PASS' ? (
        <StatusBadge tone="muted">통과</StatusBadge>
      ) : (
        <StatusBadge tone="attention">{String(c.getValue())}</StatusBadge>
      ),
  },
  {
    accessorKey: 'accuracy',
    header: '정확도',
    meta: { width: '6rem', align: 'right' },
  },
  {
    accessorKey: 'groundedness',
    header: '근거성',
    meta: { width: '6rem', align: 'right' },
  },
  {
    accessorKey: 'hallucination',
    header: '환각',
    meta: { width: '6rem' },
    cell: (c) =>
      c.getValue() ? (
        <StatusBadge tone="attention">있음</StatusBadge>
      ) : (
        <span className="text-xs text-ink-muted">없음</span>
      ),
  },
  {
    accessorKey: 'missingToolCalls',
    header: '누락 호출',
    meta: { width: '7rem', align: 'right' },
  },
  {
    accessorKey: 'latencyMs',
    header: '소요',
    meta: { width: '7rem', align: 'right' },
    cell: (c) => (
      <span className="tabular-nums text-ink-secondary">
        {(Number(c.getValue()) / 1000).toFixed(1)}초
      </span>
    ),
  },
  {
    accessorKey: 'rationale',
    header: '판정 근거',
    cell: (c) =>
      c.getValue() ? (
        <span className="line-clamp-2 text-xs text-ink-muted">{String(c.getValue())}</span>
      ) : null,
  },
];

/**
 * 실행 시작. 이미 도는 중이면 서버가 409 를 준다.
 *
 * <p>409 를 빨간 실패로 보여주지 않는다. 잘못 누른 것이 아니라 이미 돌고 있다는 뜻이고,
 * 사용자가 할 일은 기다리는 것뿐이다.
 */
/**
 * 서버의 {@code ScenarioKind} 와 같은 값이어야 한다. 이름은 {@code labels.ts} 가 붙인다.
 * 목록 열과 같은 표를 쓰므로 고르는 값과 찍히는 이름이 어긋날 자리가 없다.
 */
const SCENARIO_KINDS = ['CHAT', 'MONITOR'] as const;

function StartRunForm() {
  const [label, setLabel] = useState('');
  const [kind, setKind] = useState('');
  const start = useStartEvalRun();

  const conflict = start.error instanceof ApiError && start.error.status === 409;

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        if (label.trim() === '') {
          return;
        }
        start.mutate(
          { label: label.trim(), kind: kind || undefined },
          { onSuccess: () => setLabel('') },
        );
      }}
      className="flex items-center gap-1.5"
    >
      {conflict && <span className="text-2xs text-ink-muted">이미 실행 중입니다</span>}
      <Input
        value={label}
        onChange={(event) => setLabel(event.target.value)}
        placeholder="예: 프롬프트 v3"
        aria-label="실행 라벨"
        className="w-44"
      />
      {/* 여기 값은 시나리오의 <b>종류</b>(무엇을 채점하는가)이지 출처(어디서 왔는가)가
       * 아니다. 한때 출처 값(MANUAL, RECORDED)을 보내고 있었고, 그러면 서버가 kind 로
       * 읽다가 "알 수 없는 시나리오 kind" 로 거절해 실행 자체가 안 됐다. 두 축이 직교라
       * 이름이 비슷해도 섞이면 안 된다. */}
      <Select
        value={kind}
        onChange={(event) => setKind(event.target.value)}
        aria-label="시나리오 종류"
        className="w-28"
      >
        <option value="">전체</option>
        {SCENARIO_KINDS.map((value) => (
          <option key={value} value={value}>
            {evalKindLabel(value)}
          </option>
        ))}
      </Select>
      <Button type="submit" size="sm" disabled={label.trim() === '' || start.isPending}>
        실행
      </Button>
    </form>
  );
}

function ScenarioCard({ scenarios }: { scenarios: ReturnType<typeof useEvalScenarios> }) {
  const remove = useDeleteEvalScenario();
  const [target, setTarget] = useState<EvalScenario | null>(null);

  const columns = useMemo<ColumnDef<EvalScenario, unknown>[]>(
    () => [
      { accessorKey: 'name', header: '이름', meta: { width: '14rem' } },
      {
        accessorKey: 'kind',
        header: '종류',
        meta: { width: '8rem' },
        cell: (c) => <span className="text-xs">{evalKindLabel(String(c.getValue()))}</span>,
      },
      {
        accessorKey: 'question',
        header: '질문',
        cell: (c) => (
          <span className="line-clamp-2 text-xs text-ink-secondary">{String(c.getValue())}</span>
        ),
      },
      {
        accessorKey: 'sourceType',
        header: '출처',
        meta: { width: '8rem' },
        cell: (c) => (
          <span className="text-xs text-ink-muted">{evalSourceLabel(String(c.getValue()))}</span>
        ),
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '5rem', align: 'right' },
        cell: (c) => (
          <Button variant="danger" size="sm" onClick={() => setTarget(c.row.original)}>
            삭제
          </Button>
        ),
      },
    ],
    [],
  );

  return (
    <Card>
      <CardHeader
        title="골든셋 시나리오"
        description="여기 있는 것만 평가에 돕니다. 새 시나리오 등록은 zzolbot-evals 저장소에서 합니다."
      />
      {scenarios.isError ? (
        <ErrorState
          message={(scenarios.error as Error).message}
          onRetry={() => scenarios.refetch()}
        />
      ) : (
        <DataTable
          columns={columns}
          data={scenarios.data ?? []}
          loading={scenarios.isPending}
          emptyTitle="시나리오가 없습니다"
        />
      )}

      <ConfirmDialog
        open={target !== null}
        destructive
        title="시나리오를 삭제할까요?"
        description={`"${target?.name}" 을(를) 지웁니다. 이후 평가에서 빠지므로 과거 실행과 통과율을 나란히 비교할 수 없게 됩니다.`}
        confirmLabel="삭제"
        onConfirm={() => {
          if (target) {
            remove.mutate(target.id);
          }
          setTarget(null);
        }}
        onOpenChange={(open) => !open && setTarget(null)}
      />
    </Card>
  );
}
