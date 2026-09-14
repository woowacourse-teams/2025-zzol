import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import {
  useDeadLetters,
  useDeployment,
  useDiscardDeadLetter,
  useMigrations,
  useRequeueDeadLetter,
} from '@/api/queries';
import type { DeadLetter, DeadLetterSource, MigrationItem } from '@/api/types';
import { DataTable } from '@/components/DataTable';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { CodeBlock } from '@/components/ui/CodeBlock';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { KeyValue } from '@/components/ui/KeyValue';
import { Loaded } from '@/components/ui/Loaded';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { Timestamp } from '@/components/ui/Timestamp';

const SOURCES: { value: DeadLetterSource; label: string }[] = [
  { value: 'OUTBOX', label: '발행 실패' },
  { value: 'SETTLEMENT', label: '정산 격리' },
];

const SOURCE_HINT: Record<DeadLetterSource, string> = {
  OUTBOX:
    '재시도 10회를 소진해 발행하지 못한 메시지입니다. 원인을 고쳤다면 다시 큐에 넣을 수 있습니다.',
  SETTLEMENT:
    '소비 단계에서 격리된 정산 메시지입니다. 다시 넣는 경로를 두지 않았습니다 — 중복 반영이 곧 잘못된 정산이라, 무엇이 이미 반영됐는지 모르는 채 흘려보내는 것이 실패보다 나쁩니다.',
};

/**
 * 시스템 운영.
 *
 * <p>담는 기준은 홈과 같다. <b>Grafana 가 못 하는 것.</b> 적체 건수는 이미 게이지로
 * 나가 있다. 여기서 하는 것은 그 숫자로는 알 수 없는 둘이다. 무엇이 왜 막혔는지(원문)와
 * 그래서 어떻게 할지(재처리·폐기).
 *
 * <p>같은 이유로 Redis Stream pending 은 담지 않았다. 그것도 게이지가 있고, 밀린
 * 메시지는 스위퍼가 30초마다 스스로 회수한다. 사람이 볼 필요도 할 일도 없는 숫자를
 * 화면에 두면 나머지 숫자의 값어치까지 떨어진다.
 */
export function OpsPage() {
  const [source, setSource] = useState<DeadLetterSource>('OUTBOX');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<DeadLetter | null>(null);
  const [discardTarget, setDiscardTarget] = useState<DeadLetter | null>(null);

  const deadLetters = useDeadLetters(source, page);
  const requeue = useRequeueDeadLetter();
  const discard = useDiscardDeadLetter();

  const columns = useMemo<ColumnDef<DeadLetter, unknown>[]>(
    () => [
      {
        accessorKey: 'createdAt',
        header: '격리 시각',
        meta: { width: '13rem' },
        cell: (c) => <Timestamp value={c.getValue() as string} />,
      },
      {
        accessorKey: 'reference',
        header: source === 'OUTBOX' ? '스트림' : '레코드',
        meta: { width: '14rem' },
        cell: (c) => <span className="font-mono text-xs">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'reason',
        header: '사유',
        cell: (c) => (
          <span className="line-clamp-2 text-xs text-ink-secondary">{String(c.getValue())}</span>
        ),
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '11rem', align: 'right' },
        cell: (c) => {
          const row = c.row.original;
          return (
            <div className="flex justify-end gap-1.5">
              {row.requeueable && (
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={requeue.isPending}
                  onClick={(event) => {
                    event.stopPropagation();
                    requeue.mutate(row.id);
                  }}
                >
                  다시 넣기
                </Button>
              )}
              <Button
                variant="danger"
                size="sm"
                onClick={(event) => {
                  event.stopPropagation();
                  setDiscardTarget(row);
                }}
              >
                폐기
              </Button>
            </div>
          );
        },
      },
    ],
    [source, requeue],
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="시스템"
        description="격리된 메시지의 원문과 조치, 스키마 버전, 지금 떠 있는 빌드. 적체 건수와 스트림 지표는 Grafana(status.zzol.site)가 봅니다."
      />

      <Card>
        <CardHeader
          title="격리된 메시지"
          description={SOURCE_HINT[source]}
          actions={
            <Tabs
              tabs={SOURCES}
              value={source}
              label="격리 큐"
              onChange={(next) => {
                setSource(next);
                setPage(0);
                setSelected(null);
              }}
            />
          }
        />

        <DataTable
          error={deadLetters.error}
          onRetry={() => deadLetters.refetch()}
          columns={columns}
          data={deadLetters.data?.content ?? []}
          loading={deadLetters.isPending}
          onRowClick={(row) => setSelected(row.id === selected?.id ? null : row)}
          emptyTitle="격리된 메시지가 없습니다"
          emptyDescription="이 칸이 비어 있는 것이 정상입니다."
        />
        {deadLetters.data && (
          <Pagination
            page={deadLetters.data.page}
            totalPages={deadLetters.data.totalPages}
            totalElements={deadLetters.data.totalElements}
            onChange={setPage}
          />
        )}
      </Card>

      {selected && (
        <Card>
          <CardHeader
            title={`원문 #${selected.id}`}
            description="이 화면이 있는 이유입니다. 건수만으로는 무엇이 왜 막혔는지 알 수 없습니다."
          />
          <CardBody className="flex flex-col gap-4">
            <KeyValue
              items={[
                { label: '사유', value: selected.reason, full: true },
                {
                  label: source === 'OUTBOX' ? '스트림' : '레코드',
                  value: <span className="font-mono text-xs">{selected.reference}</span>,
                },
                {
                  label: '재시도',
                  value: selected.retryCount === null ? null : `${selected.retryCount}회`,
                },
              ]}
            />
            {selected.payload ? (
              <CodeBlock value={selected.payload} maxHeightClassName="max-h-80" />
            ) : (
              <EmptyState
                title="원문이 없습니다"
                description="파싱조차 되지 않아 본문을 남기지 못한 메시지입니다."
              />
            )}
          </CardBody>
        </Card>
      )}

      <div className="grid gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(18rem,1fr)]">
        <MigrationsCard />
        <DeploymentCard />
      </div>

      <ConfirmDialog
        open={discardTarget !== null}
        destructive
        title="이 메시지를 폐기할까요?"
        description="행을 지웁니다. 되돌릴 수 없고 원문도 함께 사라집니다. 누가 언제 지웠는지는 조치 이력에 남습니다."
        target={discardTarget ? <span className="font-mono">{discardTarget.reference}</span> : null}
        confirmLabel="폐기"
        onConfirm={() => {
          if (discardTarget) {
            discard.mutate({ source: discardTarget.source, id: discardTarget.id });
            if (selected?.id === discardTarget.id) {
              setSelected(null);
            }
          }
          setDiscardTarget(null);
        }}
        onOpenChange={(open) => !open && setDiscardTarget(null)}
      />
    </div>
  );
}

const MIGRATION_COLUMNS: ColumnDef<MigrationItem, unknown>[] = [
  {
    accessorKey: 'version',
    header: '버전',
    meta: { width: '6rem' },
    cell: (c) => <span className="font-mono text-xs">{String(c.getValue() ?? '-')}</span>,
  },
  { accessorKey: 'description', header: '설명' },
  {
    accessorKey: 'success',
    header: '결과',
    meta: { width: '7rem' },
    cell: (c) =>
      c.getValue() ? (
        <StatusBadge tone="muted">성공</StatusBadge>
      ) : (
        <StatusBadge tone="attention">실패</StatusBadge>
      ),
  },
  {
    accessorKey: 'installedOn',
    header: '적용',
    meta: { width: '13rem' },
    cell: (c) => <Timestamp value={c.getValue() as string | null} />,
  },
];

/**
 * 스키마 버전.
 *
 * <p>Boot 4 로 올릴 때 Flyway 오토컨피그 모듈이 빠져 마이그레이션이 조용히 실행되지 않은
 * 적이 있다(#1606). 그때는 어느 화면에도 티가 나지 않았다. 서버에 들어가지 않고도
 * 배포된 스키마가 코드와 맞는지 볼 수 있어야 한다.
 */
function MigrationsCard() {
  const migrations = useMigrations();

  return (
    <Card>
      {/* 건수를 받은 것에서 센다. "최근 30건" 이라고 적어 두었더니 서버의 상한과 화면의
       * 문구가 각각 30을 들고 있었고, 한쪽만 바뀌면 화면이 거짓말을 하는 상태였다.
       * 화면은 자기가 그린 것을 말하면 된다. */}
      <CardHeader
        title="스키마 버전"
        description={
          migrations.data?.managed
            ? `최신 V${migrations.data.current} · 최근 ${migrations.data.records.length}건`
            : undefined
        }
      />
      <Loaded query={migrations}>
        {(data) =>
          data.managed ? (
            <DataTable
              columns={MIGRATION_COLUMNS}
              data={data.records}
              emptyTitle="적용된 마이그레이션이 없습니다"
            />
          ) : (
            <EmptyState
              title="이 환경은 Flyway 를 쓰지 않습니다"
              description="로컬은 ddl-auto 로 스키마를 만들고 기동마다 새로 만듭니다. 이력 테이블이 없는 것이 정상입니다."
            />
          )
        }
      </Loaded>
    </Card>
  );
}

/**
 * 지금 떠 있는 것.
 *
 * <p>장애 때 가장 먼저 묻는 것이 "언제 나간 어느 커밋이냐"인데, 그동안은 배포 로그를
 * 뒤져야만 알 수 있었다.
 */
function DeploymentCard() {
  const deployment = useDeployment();

  return (
    <Card className="h-fit">
      <CardHeader
        title="배포"
        description="화면 상단 배지는 빌드 시점 값이라 여기와 어긋날 수 있습니다."
      />
      <Loaded query={deployment}>
        {(data) => (
          <CardBody>
            <KeyValue
              items={[
                { label: '버전', value: data.version },
                {
                  label: '커밋',
                  value: data.commit ? (
                    <span className="font-mono text-xs">{data.commit}</span>
                  ) : null,
                },
                { label: '빌드 시각', value: <Timestamp value={data.builtAt} /> },
                {
                  label: '활성 프로필',
                  value: <span className="font-mono text-xs">{data.profile}</span>,
                },
              ]}
            />
          </CardBody>
        )}
      </Loaded>
    </Card>
  );
}
