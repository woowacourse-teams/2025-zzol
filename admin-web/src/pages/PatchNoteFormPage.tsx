import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  usePatchNote,
  usePatchNoteCategories,
  useSavePatchNote,
  type PatchNoteForm,
} from '@/api/queries';
import type { PatchNoteCategory } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ErrorState, Skeleton } from '@/components/ui/EmptyState';
import { Input, Label, Select } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';

const CATEGORY_LABEL: Record<string, string> = {
  NOTICE: '공지',
  EVENT: '이벤트',
  UPDATE: '업데이트',
  MAINTENANCE: '점검',
};

/** 서버 제약과 같은 값이다. 여기서 먼저 막아야 다 쓰고 나서 400 을 받지 않는다. */
const MAX_TITLE = 100;
const MAX_CONTENT = 5000;

/**
 * 패치노트 작성/수정. 한 화면이 둘을 겸한다.
 *
 * <p>{@code id} 가 있으면 수정, 없으면 작성이다. 화면을 둘로 나누면 같은 폼을 두 벌
 * 유지하게 되고, 실제로 다른 것은 초기값과 저장 후 문구뿐이다.
 *
 * <p>패치노트는 유저가 보는 글이다. 여기서 오타를 내면 서비스 화면에 그대로 나간다.
 * 그래서 저장 버튼 옆에 유저가 보게 될 모습을 그대로 붙여 둔다.
 */
/**
 * 바깥 껍데기. 수정이면 기존 글을 다 받은 뒤에야 폼을 마운트한다.
 *
 * <p>폼을 먼저 띄우고 이펙트로 서버 값을 채워 넣던 것을 걷어냈다. 그러면 렌더가 한 번
 * 더 돌고, 무엇보다 "언제 덮어쓸지"를 플래그로 관리해야 한다. 플래그를 한 번이라도
 * 잘못 두면 타이핑하던 내용이 응답 도착 시점에 지워진다. 데이터를 받은 뒤에 마운트하면
 * 그 상태 자체가 없다.
 */
export function PatchNoteFormPage() {
  const params = useParams<{ id: string }>();
  const id = params.id ? Number(params.id) : null;
  const existing = usePatchNote(id);

  if (id === null) {
    return <PatchNoteForm id={null} initial={EMPTY_FORM} />;
  }

  if (existing.isPending) {
    return <Skeleton className="h-96" />;
  }

  if (existing.isError) {
    return (
      <Card>
        <ErrorState
          message={(existing.error as Error).message}
          onRetry={() => existing.refetch()}
        />
      </Card>
    );
  }

  return (
    <PatchNoteForm
      id={id}
      initial={{
        category: existing.data.category,
        title: existing.data.title,
        content: existing.data.content,
      }}
    />
  );
}

const EMPTY_FORM: PatchNoteForm = { category: 'NOTICE', title: '', content: '' };

function PatchNoteForm({ id, initial }: { id: number | null; initial: PatchNoteForm }) {
  const navigate = useNavigate();
  const categories = usePatchNoteCategories();
  const save = useSavePatchNote();

  const [form, setForm] = useState<PatchNoteForm>(initial);

  const titleOver = form.title.length > MAX_TITLE;
  const contentOver = form.content.length > MAX_CONTENT;
  const submittable =
    form.title.trim() !== '' && form.content.trim() !== '' && !titleOver && !contentOver;

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!submittable) {
      return;
    }
    save.mutate({ id, form }, { onSuccess: () => navigate('/patch-notes') });
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-6">
      <PageHeader
        title={id === null ? '패치노트 작성' : '패치노트 수정'}
        description="저장하면 유저 화면에 바로 나갑니다. 예약 발행은 없습니다."
        actions={
          <>
            <Button asChild variant="secondary" size="sm">
              <Link to="/patch-notes">취소</Link>
            </Button>
            <Button type="submit" size="sm" disabled={!submittable || save.isPending}>
              {save.isPending ? '저장 중' : '저장'}
            </Button>
          </>
        }
      />

      {/* 글을 쓰고 읽는 영역이라 여기만 좁게 잡는다. 한 줄이 화면 끝까지 가면
        * 눈이 다음 줄 첫 글자를 못 찾는다. 목록 화면이 넓은 것과 반대 이유다. */}
      <div className="grid max-w-[1100px] gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <Card>
          <CardHeader title="내용" />
          <CardBody className="flex flex-col gap-4">
            {/* 분류는 네 가지뿐이라 입력칸만큼 넓을 이유가 없다. 폭이 넓으면 고른 값과
              * 화살표 사이가 비어 무엇을 고른 것인지 한눈에 안 들어온다. */}
            <Label text="분류" hint="유저 화면에서 글을 분류하는 데 씁니다" className="items-start">
              <Select
                className="w-48"
                value={form.category}
                onChange={(event) =>
                  setForm({ ...form, category: event.target.value as PatchNoteCategory })
                }
              >
                {(categories.data ?? [form.category]).map((category) => (
                  <option key={category} value={category}>
                    {CATEGORY_LABEL[category] ?? category}
                  </option>
                ))}
              </Select>
            </Label>

            <Label text="제목" counter={`${form.title.length} / ${MAX_TITLE}`}>
              <Input
                value={form.title}
                onChange={(event) => setForm({ ...form, title: event.target.value })}
                placeholder="예: 눈치게임이 추가됐어요"
                aria-invalid={titleOver}
              />
            </Label>

            <Label text="본문" counter={`${form.content.length} / ${MAX_CONTENT}`}>
              {/* textarea 는 Field 에 없다. 이 화면에서만 쓰는 것을 프리미티브로
               * 올리면 쓰는 곳이 하나인 컴포넌트가 늘어난다. */}
              <textarea
                value={form.content}
                onChange={(event) => setForm({ ...form, content: event.target.value })}
                rows={16}
                placeholder="줄바꿈은 그대로 유지됩니다."
                aria-invalid={contentOver}
                className="w-full resize-y rounded-md border border-border-default bg-surface px-3 py-2 text-sm leading-relaxed text-ink transition-colors placeholder:text-ink-muted hover:border-ink-muted focus:border-accent focus:outline-none aria-[invalid=true]:border-attention-mark"
              />
            </Label>

            {save.isError && (
              <p className="text-xs text-attention">
                저장하지 못했습니다. {(save.error as Error).message}
              </p>
            )}
          </CardBody>
        </Card>

        <Card className="h-fit xl:sticky xl:top-[4.5rem]">
          <CardHeader
            title="유저 화면 미리보기"
            description="실제 서비스와 글꼴은 다릅니다. 줄바꿈과 길이만 봐 주세요."
          />
          <CardBody>
            <span className="inline-flex rounded-sm bg-subtle px-1.5 py-0.5 text-2xs font-medium text-ink-secondary">
              {CATEGORY_LABEL[form.category] ?? form.category}
            </span>
            <p className="mt-2 text-base font-semibold leading-snug text-ink">
              {form.title || <span className="text-ink-muted">제목이 여기 나옵니다</span>}
            </p>
            {/* whitespace-pre-wrap 이 필요하다. 서버가 줄바꿈을 그대로 저장하고
             * 유저 화면도 그대로 그린다. 여기서만 한 줄로 보이면 미리보기가 거짓말이 된다. */}
            <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-ink-secondary">
              {form.content || (
                <span className="text-ink-muted">본문이 여기 나옵니다</span>
              )}
            </p>
          </CardBody>
        </Card>
      </div>
    </form>
  );
}
