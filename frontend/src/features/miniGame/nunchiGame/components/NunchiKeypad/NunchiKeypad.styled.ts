import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

/** 버튼 톤 — 키패드 상태를 시각 톤으로 정규화한 값(요구사항 1/5). */
export type NunchiButtonTone = 'active' | 'pressed' | 'stood' | 'out' | 'muted';

// 누를 수 있을 때 바깥으로 한 겹 퍼지는 경고등 핑(긴급 표현).
const ping = keyframes`
  0% { opacity: 0.5; transform: scale(1); }
  80%, 100% { opacity: 0; transform: scale(1.14); }
`;

// STOOD 확정 순간의 작은 성취감 팝(눌린 채 유지).
const stoodPop = keyframes`
  0% { transform: translateY(4px) scale(1); }
  40% { transform: translateY(4px) scale(1.03); }
  100% { transform: translateY(4px) scale(1); }
`;

// 누르는 순간 꾹 들어가는 팝(요구사항 5) — 이후 PRESSED 가 눌린 채 유지된다.
const pressedPop = keyframes`
  0% { transform: translateY(0) scale(1); }
  45% { transform: translateY(5px) scale(0.97); }
  100% { transform: translateY(4px) scale(1); }
`;

export const Keypad = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 8px 0 18px;
`;

export const Warning = styled.div`
  width: 100%;
  padding: 8px 12px;
  border-radius: 8px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.white};
  background: ${({ theme }) => theme.color.red};
`;

/** 톤별 돔 그라디언트 3-stop(light→mid→dark). dark 가 베젤/음영을 만든다. */
const toneColors = (
  theme: { color: { point: Record<number, string>; gray: Record<number, string> } },
  tone: NunchiButtonTone
) => {
  switch (tone) {
    case 'stood':
      return {
        light: theme.color.point[400],
        mid: theme.color.point[500],
        dark: theme.color.point[500],
      };
    case 'pressed':
      return {
        light: theme.color.point[200],
        mid: theme.color.point[300],
        dark: theme.color.point[400],
      };
    case 'out':
    case 'muted':
      return {
        light: theme.color.gray[300],
        mid: theme.color.gray[400],
        dark: theme.color.gray[500],
      };
    case 'active':
    default:
      return {
        light: theme.color.point[300],
        mid: theme.color.point[400],
        dark: theme.color.point[500],
      };
  }
};

export const PressButton = styled.button<{
  $tone: NunchiButtonTone;
  $invite: boolean;
}>`
  position: relative;
  /* 원형 긴급 버튼(요구사항 4) — 큰 타겟(요구사항 5). */
  width: clamp(184px, 58vw, 216px);
  aspect-ratio: 1 / 1;
  border: none;
  border-radius: 50%;
  cursor: pointer;

  /* 더블탭 줌·텍스트 선택·길게누르기 방지(요구사항 5/J). */
  touch-action: manipulation;
  user-select: none;
  -webkit-user-select: none;
  -webkit-tap-highlight-color: transparent;

  transition:
    transform 0.1s ease,
    box-shadow 0.1s ease,
    background 0.2s ease;

  ${({ theme, $tone }) => {
    const { light, mid, dark } = toneColors(theme, $tone);
    // 글로시 돔 + 동심원 타겟 그루브(가운데 디테일) + 온-브랜드 레드 베젤 + 헤어라인 + 입체 음영.
    return css`
      background:
        radial-gradient(
          circle at 50% 50%,
          transparent 30%,
          ${dark}1F 31%,
          ${dark}1F 33%,
          transparent 34%,
          transparent 55%,
          ${dark}29 56%,
          ${dark}29 58%,
          transparent 59%
        ),
        radial-gradient(circle at 50% 34%, ${light} 0%, ${mid} 55%, ${dark} 100%);
      box-shadow:
        0 0 0 6px ${dark},
        0 0 0 7px ${theme.color.gray[300]},
        0 13px 20px ${mid}59,
        inset 0 9px 13px ${theme.color.white}4D,
        inset 0 -12px 16px ${theme.color.black}26;
    `;
  }}

  /* 누르는 순간 살짝 들어간다(요구사항 1/4). */
  &:active:not(:disabled) {
    transform: translateY(4px);
    ${({ theme, $tone }) => {
      const { light, mid, dark } = toneColors(theme, $tone);
      return css`
        background: radial-gradient(circle at 50% 40%, ${mid} 0%, ${dark} 70%, ${dark} 100%);
        box-shadow:
          0 0 0 6px ${dark},
          0 0 0 7px ${theme.color.gray[300]},
          0 5px 10px ${mid}40,
          inset 0 7px 12px ${theme.color.black}33,
          inset 0 -4px 10px ${light}33;
      `;
    }}
  }

  /* PRESSED: 누르는 순간 꾹 들어가고 눌린 채 대기 — "내가 눌렀고 잠겼다"는 촉각 신호(요구사항 5/E).
     :active 는 즉시 disabled 되며 끊기므로, React 가 관리하는 pressed 톤으로 클릭감을 표현한다. */
  ${({ theme, $tone }) =>
    $tone === 'pressed' &&
    css`
      transform: translateY(4px);
      box-shadow:
        0 0 0 6px ${theme.color.point[400]},
        0 0 0 7px ${theme.color.gray[300]},
        0 5px 10px ${theme.color.point[400]}40,
        inset 0 7px 12px ${theme.color.black}33,
        inset 0 -4px 10px ${theme.color.point[200]}33;
      animation: ${pressedPop} 0.2s ease-out;
    `}

  /* STOOD: 눌린 채 고정 — "확정" 됐다는 촉각적 신호. */
  ${({ theme, $tone }) =>
    $tone === 'stood' &&
    css`
      transform: translateY(4px);
      box-shadow:
        0 0 0 6px ${theme.color.point[500]},
        0 0 0 7px ${theme.color.gray[300]},
        0 5px 10px ${theme.color.point[500]}40,
        inset 0 7px 12px ${theme.color.black}2E;
      animation: ${stoodPop} 0.4s ease-out;
    `}

  /* 돔 위 유리 반사광(스페큘러 하이라이트). */
  &::before {
    content: '';
    position: absolute;
    top: 11%;
    left: 23%;
    width: 50%;
    height: 30%;
    border-radius: 50%;
    pointer-events: none;
    background: radial-gradient(
      ellipse at center,
      ${({ theme }) => theme.color.white}99 0%,
      ${({ theme }) => theme.color.white}00 70%
    );
  }

  /* 누를 수 있을 때만 바깥으로 퍼지는 경고등 핑. */
  &::after {
    content: '';
    position: absolute;
    inset: -7px;
    border-radius: 50%;
    pointer-events: none;
    border: 4px solid ${({ theme }) => theme.color.point[300]};
    opacity: 0;
    ${({ $tone, $invite }) =>
      $tone === 'active' &&
      $invite &&
      css`
        animation: ${ping} 1.6s ease-in-out infinite;
      `}
  }

  &:disabled {
    cursor: not-allowed;
  }

  @media (prefers-reduced-motion: reduce) {
    transition: background 0.2s ease;
    animation: none;
    &::after {
      animation: none;
    }
  }
`;
