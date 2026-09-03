import styled from '@emotion/styled';
import { PIXELS_PER_UNIT } from '../../constants/track';
import { RACING_Z_INDEX } from '../../constants/zIndex';

const TRANSITION_DURATION_MS = 100;

type Props = {
  $isMe: boolean;
  $position: number;
  $myPosition: number;
};

export const Container = styled.div<Props>`
  position: relative;
  transform: ${({ $isMe, $position, $myPosition }) => {
    if ($isMe) return 'translateX(0)';
    const relativeX = ($position - $myPosition) * PIXELS_PER_UNIT;
    return `translateX(${relativeX}px)`;
  }};
  transition: transform ${TRANSITION_DURATION_MS}ms linear;
  z-index: ${RACING_Z_INDEX.PLAYER};
`;

type RingProps = {
  $borderStyle: string;
  $isMe: boolean;
};

/**
 * 색 말고 형태로도 사람을 가른다. 적록색약 사용자와, 명단이 복구되기 전 전원이 같은 색으로
 * 떨어지는 구간이 대상이다. 테두리는 회전하지 않아야 읽히므로 도는 아이콘 바깥에 둔다.
 */
export const IconRing = styled.div<RingProps>`
  display: flex;
  border: 3px ${({ $borderStyle }) => $borderStyle} ${({ theme }) => theme.color.white};
  border-radius: 50%;
  box-shadow: ${({ theme, $isMe }) => ($isMe ? `0 0 0 3px ${theme.color.white}47` : 'none')};
  ${({ $borderStyle }) => $borderStyle === 'double' && 'border-width: 4px;'}
`;

export const RotatingWrapper = styled.div`
  will-change: transform;
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

/** 색과 무관하게 내가 누구인지 알리는 표식. 이름 위에 항상 띄운다. */
export const MyMarker = styled.span`
  position: absolute;
  top: -38px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 7px solid transparent;
  border-right: 7px solid transparent;
  border-bottom: 9px solid ${({ theme }) => theme.color.white};
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.85));
`;
