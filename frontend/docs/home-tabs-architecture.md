# 홈 탭 구조 설계

## 탭 구성

게임 / 랭킹 / 건의사항 순서로 하단 탭바 배치.

- **게임 탭**: 기존 대시보드 배너 + 방 만들기/참가 버튼
- **랭킹 탭**: 아코디언 형태 랭킹 카테고리 목록
- **건의사항 탭**: step 기반 카테고리 선택 → 내용 입력 플로우

게임 탭 외에는 상단 배너(DashBoard)가 숨겨져 콘텐츠가 전체 높이를 사용한다.

---

## 하단 탭바 구현 — flex last-item (position: fixed 미사용)

`LayoutContainer`가 `padding: 1rem`을 갖기 때문에 `position: fixed`로 구현하면 콘텐츠 하단에 별도 padding 계산이 필요해진다.

대신 탭바를 Layout flex 컨테이너의 마지막 자식으로 두고, negative margin으로 Layout padding을 상쇄해 화면 엣지까지 확장한다.

```typescript
// HomeTabs.styled.ts
export const TabBar = styled.nav`
  flex-shrink: 0;
  margin: 0 -1rem -1rem;   // Layout padding(1rem) 상쇄
  padding-bottom: env(safe-area-inset-bottom);  // iOS 안전 영역
`;
```

`height: 100%`인 LayoutContainer에서 탭바가 항상 하단에 위치하므로 시각적 결과는 `position: fixed`와 동일하다.

---

## 랭킹 탭 — Config-driven 아코디언

카테고리가 늘어나도 컴포넌트 수정 없이 config만 추가하면 된다.

**카테고리 추가 방법**: `src/features/home/config/rankingConfigs.ts`의 `RANKING_CATEGORIES` 배열에 항목 추가.

```typescript
{
  key: 'new-category',
  label: '새 카테고리',
  icon: '🆕',
  endpoint: '/dashboard/new-endpoint',
  transformData: (raw) =>
    (raw as NewType[]).map((item, i) => ({
      rank: i + 1,
      name: item.name,
      count: item.value,
      unit: '회',  // 단위 커스텀 가능: '회', '%', 'ms' 등
    })),
}
```

현재 카테고리:
- `top-winners` — 이번 달 당첨 랭킹 (unit: 회)
- `lowest-probability` — 최저 확률 당첨자 (unit: %)
- `game-play-counts` — 게임 인기 순위 (unit: 회)

데이터는 아코디언 열릴 때 `useLazyFetch`로 lazy 로딩된다. 카테고리를 닫았다 다시 열어도 이미 fetch한 데이터는 재요청하지 않는다.

---

## 건의사항 탭 — Step 플로우

```
category → (BUG일 때) game-select → form → success
         → (INFO일 때) info
         → (나머지) form → success
```

- 돌아가기 버튼은 `category`, `success` step에서는 미표시
- `form` step에서 BUG 카테고리면 `game-select`로, 나머지는 `category`로 복귀
- 제출 후 `success` step으로 전환, "다시 작성하기"로 초기화

백엔드 API 미구현 상태. 연동 방법은 `docs/tab-api-todo.md` 참고.
