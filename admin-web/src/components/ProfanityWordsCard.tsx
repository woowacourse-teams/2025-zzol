import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { useAddProfanityWord, useProfanityWords, useToggleProfanityWord } from '@/api/queries';
import type { ProfanityWord } from '@/api/types';
import { DataTable } from '@/components/DataTable';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Input, SearchInput, Select } from '@/components/ui/Field';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useDebounced } from '@/lib/useDebounced';

const LANGUAGE_LABEL: Record<string, string> = { KOREAN: '한국어', ENGLISH: '영어' };
/** 서버 {@code WordSource} 와 같은 값이다. 모르는 값이 오면 원문을 그대로 보여준다. */
const SOURCE_LABEL: Record<string, string> = {
  VANE: '기본 사전(vane)',
  LDNOOBW: '기본 사전(LDNOOBW)',
  MANUAL: '직접 추가',
  AI_FLAGGED: 'AI 판정',
  OPERATOR_ALLOWED: '운영자 허용',
};

/**
 * 금칙어 사전.
 *
 * <p>AI 판정 앞단에서 먼저 걸리는 목록이다. 검열 큐에 같은 닉네임이 반복해서 올라오면
 * 여기 한 줄 넣는 것이 모델을 손보는 것보다 빠르다.
 *
 * <p><b>지우지 않고 끈다.</b> 삭제 API 를 쓰지 않는 이유는, 지우면 왜 걸렸던 단어인지가
 * 사라져 같은 단어를 두 번 추가하고 두 번 푸는 일이 반복되기 때문이다. 서버도
 * activate/deactivate 만 열어 두었다.
 */
export function ProfanityWordsCard() {
  const [search, setSearch] = useState('');
  const [language, setLanguage] = useState('');
  const [active, setActive] = useState('');
  const [page, setPage] = useState(0);

  const debouncedSearch = useDebounced(search);

  const words = useProfanityWords({
    search: debouncedSearch || undefined,
    language: language || undefined,
    active: active === '' ? undefined : active === 'true',
    page,
  });

  const toggle = useToggleProfanityWord();

  const columns = useMemo<ColumnDef<ProfanityWord, unknown>[]>(
    () => [
      {
        accessorKey: 'word',
        header: '단어',
        cell: (c) => <span className="font-medium">{String(c.getValue())}</span>,
      },
      {
        accessorKey: 'language',
        header: '언어',
        meta: { width: '7rem' },
        cell: (c) => (
          <span className="text-ink-secondary">
            {LANGUAGE_LABEL[String(c.getValue())] ?? String(c.getValue())}
          </span>
        ),
      },
      {
        accessorKey: 'source',
        header: '출처',
        meta: { width: '11rem' },
        cell: (c) => (
          <span className="text-ink-secondary">
            {SOURCE_LABEL[String(c.getValue())] ?? String(c.getValue())}
          </span>
        ),
      },
      {
        accessorKey: 'active',
        header: '상태',
        meta: { width: '7rem' },
        cell: (c) =>
          c.getValue() ? (
            <StatusBadge tone="neutral">적용 중</StatusBadge>
          ) : (
            <StatusBadge tone="muted">꺼짐</StatusBadge>
          ),
      },
      {
        id: 'actions',
        header: '',
        meta: { width: '6rem', align: 'right' },
        cell: (c) => {
          const row = c.row.original;
          // 둘 다 secondary 다. 끄기를 danger 로 두었더니 스무 행이 통째로 코랄 테두리가
          // 되어 화면이 경고로 뒤덮였고, 바로 위 검열 대기의 "차단" 과 같은 무게로 읽혔다.
          // 차단은 되돌리려면 사람이 다시 판정해야 하지만 끄기는 옆 버튼 한 번으로
          // 돌아온다. 되돌릴 수 있는 조치에는 색을 주지 않는다.
          return (
            <Button
              variant="secondary"
              size="sm"
              disabled={toggle.isPending}
              onClick={() => toggle.mutate({ word: row.word, active: !row.active })}
            >
              {row.active ? '끄기' : '켜기'}
            </Button>
          );
        },
      },
    ],
    [toggle],
  );

  return (
    <Card>
      <CardHeader
        title="금칙어 사전"
        description="AI 판정 앞단에서 먼저 걸립니다. 지우지 않고 끄기만 합니다."
        actions={<AddWordForm />}
      />

      <CardBody className="flex flex-wrap items-center gap-2 border-b border-border-default">
        <SearchInput
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
          placeholder="단어 검색"
          className="w-56"
        />
        <Select
          value={language}
          onChange={(event) => {
            setLanguage(event.target.value);
            setPage(0);
          }}
          className="w-32"
        >
          <option value="">전체 언어</option>
          <option value="KOREAN">한국어</option>
          <option value="ENGLISH">영어</option>
        </Select>
        <Select
          value={active}
          onChange={(event) => {
            setActive(event.target.value);
            setPage(0);
          }}
          className="w-32"
        >
          <option value="">전체 상태</option>
          <option value="true">적용 중</option>
          <option value="false">꺼짐</option>
        </Select>
      </CardBody>

      <DataTable
        error={words.error}
        onRetry={() => words.refetch()}
        columns={columns}
        data={words.data?.content ?? []}
        loading={words.isPending}
        emptyTitle="단어가 없습니다"
        emptyDescription="검색 조건을 지우거나 새 단어를 추가해 보세요."
      />
      {words.data && (
        <Pagination
          page={words.data.page}
          totalPages={words.data.totalPages}
          totalElements={words.data.totalElements}
          onChange={setPage}
        />
      )}
    </Card>
  );
}

/**
 * 새 단어 추가. 카드 머리에 한 줄로 둔다.
 *
 * <p>다이얼로그를 띄우지 않는다. 입력이 둘뿐이고, 검열 큐를 보다가 "이건 사전에 넣자"
 * 하는 순간이 잦아 화면을 가리지 않는 편이 낫다.
 */
function AddWordForm() {
  const [word, setWord] = useState('');
  const [language, setLanguage] = useState<ProfanityWord['language']>('KOREAN');
  const add = useAddProfanityWord();

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = word.trim();
    if (trimmed === '') {
      return;
    }
    add.mutate({ word: trimmed, language }, { onSuccess: () => setWord('') });
  };

  return (
    <form onSubmit={submit} className="flex items-center gap-1.5">
      <Input
        value={word}
        onChange={(event) => setWord(event.target.value)}
        placeholder="추가할 단어"
        aria-label="추가할 단어"
        className="w-44"
      />
      <Select
        value={language}
        onChange={(event) => setLanguage(event.target.value as ProfanityWord['language'])}
        aria-label="언어"
        className="w-24"
      >
        <option value="KOREAN">한국어</option>
        <option value="ENGLISH">영어</option>
      </Select>
      <Button type="submit" size="sm" disabled={word.trim() === '' || add.isPending}>
        추가
      </Button>
    </form>
  );
}
