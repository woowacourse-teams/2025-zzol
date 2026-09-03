import styled from '@emotion/styled';
import { PIXELS_PER_UNIT, ROW_MOVE_MS, ROW_PITCH_PX } from '../../constants/track';
import { RACING_Z_INDEX } from '../../constants/zIndex';

const TRANSITION_DURATION_MS = 100;

type Props = {
  $isMe: boolean;
  $position: number;
  $myPosition: number;
};

type SlotProps = {
  $slot: number;
  $isVisible: boolean;
  $reduceMotion: boolean;
};

/**
 * 세로 자리를 잡는 층. 나는 늘 슬롯 0이라 화면 세로 가운데에 고정된다.
 *
 * 흐름에서 빼고 슬롯 번호로 배치해야 추월할 때 두 행이 스르륵 자리를 바꾼다. flex 순서만 바꾸면
 * 즉시 튄다. 잘린 사람은 언마운트하지 않고 가장자리 밖 슬롯에 숨겨, 들어오고 나가는 것도 같은
 * 이동 애니메이션으로 처리한다.
 */
export const Slot = styled.div<SlotProps>`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, calc(-50% + ${({ $slot }) => $slot * ROW_PITCH_PX}px));
  opacity: ${({ $isVisible }) => ($isVisible ? 1 : 0)};
  pointer-events: none;
  transition: ${({ $reduceMotion }) =>
    $reduceMotion ? 'none' : `transform ${ROW_MOVE_MS}ms ease, opacity ${ROW_MOVE_MS}ms ease`};
  z-index: ${RACING_Z_INDEX.PLAYER};
`;

export const Container = styled.div<Props>`
  position: relative;
  transform: ${({ $isMe, $position, $myPosition }) => {
    if ($isMe) return 'translateX(0)';
    const relativeX = ($position - $myPosition) * PIXELS_PER_UNIT;
    return `translateX(${relativeX}px)`;
  }};
  transition: transform ${TRANSITION_DURATION_MS}ms linear;
`;

type RingProps = {
  $borderStyle: string;
};

/**
 * 색 말고 형태로도 사람을 가른다. 적록색약 사용자와, 명단이 복구되기 전 전원이 같은 색으로
 * 떨어지는 구간이 대상이다. 테두리는 회전하지 않아야 읽히므로 도는 아이콘 바깥에 둔다.
 *
 * 내 것에만 글로우를 두르지 않는다. 내 캐릭터는 화면 정중앙에 고정돼 있어 글로우를 얹으면
 * 흰 링이 둘린 원이 늘 가운데 떠 있게 되고 탭 버튼으로 읽힌다. 나를 짚는 표식은 머리 위
 * 삼각형 하나로 충분하다.
 */
export const IconRing = styled.div<RingProps>`
  display: flex;
  border: 3px ${({ $borderStyle }) => $borderStyle} ${({ theme }) => theme.color.white};
  border-radius: 50%;
  ${({ $borderStyle }) => $borderStyle === 'double' && 'border-width: 4px;'}
`;

export const RotatingWrapper = styled.div`
  will-change: transform;
`;

/**
 * 캐릭터 뒤로 흐르는 속도선. 진하기는 style 로 넘긴다. 매 틱 값이 달라 prop 으로 주면
 * 틱마다 emotion 클래스가 하나씩 쌓인다.
 *
 * 아이콘을 두르지 않고 뒤에만 깐다. 정중앙에 고정된 원을 감싸면 탭 버튼으로 읽힌다.
 */
export const SpeedTrail = styled.span`
  position: absolute;
  top: 50%;
  right: calc(100% + 4px);
  transform: translateY(-50%);
  width: 34px;
  height: 26px;
  pointer-events: none;
  background: repeating-linear-gradient(
    to bottom,
    ${({ theme }) => theme.color.white}CC 0 2px,
    transparent 2px 9px
  );
  /* 오른쪽으로 갈수록 진해져 캐릭터에서 뻗어 나온 것으로 보인다. */
  -webkit-mask-image: linear-gradient(to right, transparent, ${({ theme }) => theme.color.black});
  mask-image: linear-gradient(to right, transparent, ${({ theme }) => theme.color.black});
`;

export const PlayerName = styled.div`
  position: absolute;
  top: -18px;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  width: 100%;
  /* 밝은 하늘 위의 흰 글자라 야외에서 흐려진다. */
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.85);
`;

/**
 * 색과 무관하게 내가 누구인지 알리는 표식. 이름 위에 항상 띄운다.
 *
 * 시안의 SVG 는 위를 가리키는데 머리 위 표식이 위를 보면 아무것도 안 짚는다. 아래로 뒤집었다.
 */
export const MyMarker = styled.span`
  position: absolute;
  top: -38px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 7px solid transparent;
  border-right: 7px solid transparent;
  border-top: 9px solid ${({ theme }) => theme.color.white};
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.85));
`;
