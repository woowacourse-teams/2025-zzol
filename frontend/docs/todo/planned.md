# 예정

구현 확정, 우선순위 결정된 작업 목록.
작업 완료 시 `done.md`로 이동한다.

---

## 패치 내역 GET /patch-notes API 연동

`MenuTab/views/PatchNotesView.tsx`에 정적 데이터 하드코딩 중.

**변경 파일**: `src/features/home/components/MenuTab/views/PatchNotesView.tsx`
- 파일 상단 `PATCH_NOTES` 상수 제거
- `useFetch` 훅으로 교체

```typescript
const { data, loading } = useFetch<PatchNote[]>('/patch-notes');
```

엔드포인트 스펙은 `docs/api-design-menu-tab.md` 참고.
