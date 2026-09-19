import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { useMonitorAlerts } from '@/api/queries';
import type { MonitorAlert } from '@/api/types';
import { DataTable } from '@/components/DataTable';
import { alertSeverityLabel } from '@/lib/labels';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { CodeBlock } from '@/components/ui/CodeBlock';
import { KeyValue } from '@/components/ui/KeyValue';
import { StatusBadge } from '@/components/ui/StatusBadge';

/**
 * 무인 모니터링 결과.
 *
 * <p>Alertmanager 웹훅으로 들어온 신호를 ZzolBot 이 분석한 기록이다. Grafana 는
 * "무엇이 튀었나"까지 보여주고, 여기는 "그게 무슨 뜻이고 무엇을 해야 하나"가 남는다.
 *
 * <p>목록에서 한 줄을 고르면 아래에 원문이 펼쳐진다. 신호와 제안은 JSON 문자열
 * 그대로 저장돼 있어, 화면이 해석하지 않고 그대로 보여준다. 필드를 골라 표로 만들면
 * 모델이 새 필드를 넣기 시작한 순간부터 화면이 조용히 정보를 버린다.
 */
export function ZzolBotMonitorPanel() {
  const alerts = useMonitorAlerts();
  const [selected, setSelected] = useState<MonitorAlert | null>(null);

  const columns = useMemo<ColumnDef<MonitorAlert, unknown>[]>(
    () => [
      {
        accessorKey: 'createdAt',
        header: '시각',
        meta: { width: '9rem' },
        cell: (c) => <span className="tabular-nums text-ink-secondary">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'anomalous',
        header: '판정',
        meta: { width: '8rem' },
        cell: (c) =>
          c.getValue() ? (
            <StatusBadge tone="attention">이상</StatusBadge>
          ) : (
            <StatusBadge tone="muted">정상</StatusBadge>
          ),
      },
      {
        accessorKey: 'severity',
        header: '심각도',
        meta: { width: '7rem' },
        cell: (c) => <span className="text-xs">{alertSeverityLabel(String(c.getValue()))}</span>,
      },
      {
        accessorKey: 'analysisSummary',
        header: '분석 요약',
        cell: (c) =>
          c.getValue() ? (
            <span className="line-clamp-2 text-xs text-ink-secondary">{String(c.getValue())}</span>
          ) : (
            <span className="text-ink-muted">-</span>
          ),
      },
      {
        accessorKey: 'notified',
        header: '알림',
        meta: { width: '7rem' },
        cell: (c) =>
          c.getValue() ? (
            <span className="text-xs text-ink-secondary">발송</span>
          ) : (
            <span className="text-xs text-ink-muted">보류</span>
          ),
      },
    ],
    [],
  );

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader
          title="모니터링 분석"
          description="30초마다 다시 읽습니다. 줄을 누르면 원문이 아래에 펼쳐집니다."
        />
        <DataTable
          error={alerts.error}
          onRetry={() => alerts.refetch()}
          columns={columns}
          data={alerts.data ?? []}
          loading={alerts.isPending}
          onRowClick={setSelected}
          emptyTitle="분석 기록이 없습니다"
          emptyDescription="Alertmanager 웹훅이 들어오면 여기에 쌓입니다."
        />
      </Card>

      {selected && (
        <Card>
          <CardHeader
            title={`분석 #${selected.id}`}
            description={selected.createdAt}
          />
          <CardBody className="flex flex-col gap-4">
            <KeyValue
              items={[
                { label: '심각도', value: alertSeverityLabel(selected.severity) },
                {
                  label: '지문',
                  value: selected.fingerprint ? (
                    <span className="font-mono text-xs">{selected.fingerprint}</span>
                  ) : null,
                },
                {
                  label: '요약',
                  value: selected.analysisSummary,
                  full: true,
                },
              ]}
            />
            {selected.signalsJson && (
              <div>
                <p className="mb-1.5 text-xs font-medium text-ink-secondary">신호 원문</p>
                <CodeBlock value={selected.signalsJson} maxHeightClassName="max-h-64" />
              </div>
            )}
            {selected.suggestedActionsJson && (
              <div>
                <p className="mb-1.5 text-xs font-medium text-ink-secondary">제안된 조치</p>
                <CodeBlock value={selected.suggestedActionsJson} maxHeightClassName="max-h-64" />
              </div>
            )}
          </CardBody>
        </Card>
      )}
    </div>
  );
}
