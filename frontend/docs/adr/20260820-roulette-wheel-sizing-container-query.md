---
date: 2026-08-20
status: accepted
---

# 룰렛 휠 크기를 컨테이너 쿼리 단위로 잡고, 대기방만 폭 기준으로 둔다

## 맥락

룰렛 휠은 `300px` 고정이었다. 390×844 화면에서는 가로로 58px을 남기고, 320×568에서는 세로가
모자라 확률 텍스트를 밀어냈다(`Layout.Content` 가 `overflow: hidden`). 한 값이 큰 화면에서는
여백을, 작은 화면에서는 잘림을 만들었다.

휠은 정사각형이라 "남는 공간에 들어가는 가장 큰 정사각형"이 필요하다. CSS의 `max-width`·
`max-height` 는 한 축을 자를 때 다른 축에 되먹임되지 않아 이 계산을 표현하지 못한다.

## 결정

**룰렛 플레이 화면**은 휠을 감싸는 영역을 컨테이너로 삼고 컨테이너 쿼리 단위로 크기를 정한다.

```ts
// RoulettePlaySection.styled.ts
RouletteWheelArea   → flex: 1; min-height: 0; container-type: size;
RouletteWheelWrapper→ width: 100%;                   /* 미지원 폴백 */
                      width: min(100cqw, 100cqh);
                      aspect-ratio: 1;
```

**대기방**은 같은 방식을 쓰지 않는다. 폭 기준(`width: 100%; aspect-ratio: 1`)으로 둔다.

## 고려한 대안

- **`ResizeObserver` 훅으로 높이를 재서 px 계산** — 동작은 하지만 CSS로 끝나는 일을 JS로
  가져온다. 레이아웃 계산이 렌더 이후로 밀려 첫 프레임에 크기가 튄다.
- **뷰포트 높이 기반 `calc()` 매직넘버** — 상단바·버튼바·리스트 높이를 손으로 빼야 한다.
  이 PR이 걷어낸 `calc(1rem + 42px)` 와 같은 종류의 부채를 다시 만든다.
- **대기방도 컨테이너 쿼리로 통일** — `container-type: size` 는 `contain: size` 를 함의해
  요소가 내용으로 커지지 않는다. 대기방 섹션(`LobbyPage.styled.ts` 의 `SectionContent`)은
  `flex: 1; overflow-y: auto` 인 **블록**이라 남은 높이가 확정되지 않는다. 그 안에서
  `container-type: size` 를 쓰면 높이가 0으로 접혀 휠이 사라진다.

## 결과 및 영향

- 실측(390×844 기준 콘텐츠 712px): 휠 358×358, 배정되지 않은 빈 공간 0px.
  320×568에서는 휠이 258px로 줄며 잘림이 사라진다.
- **높이 사슬에 의존한다.** `Layout.Content(flex:1)` → 페이지 `Container(height:100%)` →
  섹션 `Container(flex:1, min-height:0)` → `RouletteWheelArea(flex:1, min-height:0)` 로
  이어진다. 이 사슬 어디서든 확정 높이가 끊기면 휠이 조용히 0으로 렌더된다.
  고정 300px 시절에는 없던 실패 모드다. 상위 레이아웃을 바꿀 때 주의한다.
- **폴백이 필수다.** 컨테이너 쿼리 단위를 모르는 브라우저는 `width` 선언을 통째로 버린다.
  휠의 자식은 전부 `position: absolute`(`Flip`)라 내용 폭이 0이 되고, `aspect-ratio` 가
  높이까지 0으로 만들어 휠이 사라진다. 앞줄의 `width: 100%` 가 이를 막는다.
- 지원 범위: Chrome 105+ / Safari 16+ / Firefox 110+. `package.json` 에 `browserslist` 가
  없어 빌드 타깃이 명시돼 있지 않다. 지원 하한을 정하게 되면 이 결정을 다시 본다.
- 같은 휠을 두 화면이 다르게 재는 상태다. 대기방 섹션이 확정 높이를 갖는 구조로 바뀌면
  플레이 화면과 통일할 수 있다.
