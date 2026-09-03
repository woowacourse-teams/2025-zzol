import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

const FILL_TRANSITION_DURATION = 100;

export const Container = styled.div`
  width: 100%;
  position: relative;
  /* 노치가 있는 기기에서 마커 윗부분이 상태 표시줄에 물린다. */
  padding: calc(1.8rem + env(safe-area-inset-top)) 1rem 0 1rem;
`;

/** 마커를 트랙 위에 걸치지 않고 제 줄에 세운다. 겹쳐 있으면 첫 글자가 안 읽힌다. */
export const MarkerRow = styled.div`
  position: relative;
  height: 24px;
  margin-bottom: 8px;
`;

export const ProgressTrack = styled.div`
  position: relative;
  width: 100%;
  height: 15px;
  background-color: ${({ theme }) => theme.color.white}47;
  border-radius: 10px;
`;

/** 채움 막대를 둥근 트랙 모양으로 잘라 준다. */
export const FillClip = styled.div`
  position: absolute;
  inset: 0;
  border-radius: 10px;
  overflow: hidden;
`;

type FillProps = {
  $color: string;
};

export const ProgressFill = styled.div<FillProps>`
  position: absolute;
  inset: 0;
  transform-origin: left center;
  background-color: ${({ $color }) => $color};
  /* width 를 애니메이션하면 100ms 마다 레이아웃이 다시 돈다. scaleX 는 컴포지터에서 끝난다. */
  transition: transform ${FILL_TRANSITION_DURATION}ms ease-out;
  z-index: ${RACING_Z_INDEX.PROGRESS_BAR};
`;

/**
 * 트랙과 같은 폭을 가진 빈 층. translateX 의 퍼센트가 트랙 폭 기준이 되어
 * left 대신 transform 으로 마커를 옮길 수 있다.
 */
export const Anchor = styled.div`
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 0;
  transition: transform ${FILL_TRANSITION_DURATION}ms ease-out;
`;

/** 채움 막대 끝에 세우는 내 위치 표시. 색을 못 읽어도 어디까지 왔는지 보인다. */
export const MyPositionTick = styled.div`
  position: absolute;
  left: 0;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 3px;
  height: 23px;
  border-radius: 2px;
  background: ${({ theme }) => theme.color.white};
  z-index: ${RACING_Z_INDEX.PROGRESS_BAR_ME};
`;

type MarkerProps = {
  $color: string;
  $isMe: boolean;
};

export const ProgressMarker = styled.div<MarkerProps>`
  position: absolute;
  left: 0;
  top: ${({ $isMe }) => ($isMe ? '0' : '3px')};
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: ${({ $isMe }) => ($isMe ? '24px' : '18px')};
  height: ${({ $isMe }) => ($isMe ? '24px' : '18px')};
  background-color: ${({ $color }) => $color};
  border: ${({ theme, $isMe }) => ($isMe ? `2px solid ${theme.color.white}` : 'none')};
  border-radius: 50%;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
  z-index: ${({ $isMe }) =>
    $isMe ? RACING_Z_INDEX.PROGRESS_MARKER_ME : RACING_Z_INDEX.PROGRESS_MARKER};
`;

type InitialProps = {
  $onLightColor: boolean;
};

/** 색을 못 읽어도 누구인지 알 수 있게 이름 첫 글자를 넣는다. */
export const MarkerInitial = styled.span<InitialProps>`
  ${({ theme }) => theme.typography.caption}
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  line-height: 1;
  color: ${({ theme, $onLightColor }) =>
    $onLightColor ? theme.color.gray[950] : theme.color.white};
`;
