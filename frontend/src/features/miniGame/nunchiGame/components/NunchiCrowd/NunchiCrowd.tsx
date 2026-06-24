import PlayerIcon from '@/components/@composition/PlayerIcon/PlayerIcon';
import Description from '@/components/@common/Description/Description';
import { colorList, ColorKey } from '@/constants/color';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { memo } from 'react';
import * as S from './NunchiCrowd.styled';

/** 무대 위 한 사람의 상태. */
export type NunchiPersonState = 'seated' | 'stood' | 'out';

const nameColor = (state: NunchiPersonState, isMe: boolean): ColorKey => {
  if (isMe) return 'point-500';
  if (state === 'out') return 'gray-400';
  return 'gray-700';
};

// 명단이 없을 때(하드 리프레시·직접 진입)는 getParticipantColorIndex 가 모두 0 을 반환해 전원이
// 같은 색이 된다. 이름 해시로 안정적인 색 인덱스를 만들어 사람마다 구분되게 폴백한다.
const fallbackColorIndex = (name: string): number => {
  let hash = 0;
  for (let i = 0; i < name.length; i += 1) {
    hash = (hash * 31 + name.charCodeAt(i)) % colorList.length;
  }
  return hash;
};

/**
 * 눈치게임 무대 위 사람들(방향 A — "무대 위 사람들").
 *
 *  - 참가자(최대 9명)를 아바타로 그려 "누가 남았는지"를 한눈에 보여준다.
 *  - 앉음(seated): 아직 안 누른 사람 — 살짝 흐리게. 모여서 "남은 N명" 긴장을 만든다.
 *  - 일어섬(stood): 누른 사람 — 아바타 위에 누른 숫자 뱃지를 띄운다. lastStand 만 뱃지 pop(요구사항 7).
 *  - 탈락(out): 충돌(collided)한 사람 — 회색·기울어짐(번호 미표시). 쿨다운 연출은 Stage 가 담당한다.
 *
 * 번호는 stand 이벤트로 라이브 수집한 standNumbers 에서 읽는다. 재접속 전에 일어선 사람은 번호를
 * 모를 수 있으므로(스냅샷 stood 는 이름만) 그 경우만 숫자 대신 중립 표시('•')로 폴백한다.
 *
 * 명단(participants)은 로비에서 채워져 게임까지 유지된다. 하드 리프레시·플레이 직접 진입 시엔
 * 비어 있을 수 있으므로 graceful degradation: 명단이 없으면 일어선/탈락한 사람만 그리고
 * "남은 N명" 은 숨긴다(앉은 군중을 알 수 없으므로).
 */
const NunchiCrowd = () => {
  const { myName } = useIdentifier();
  const { participants, getParticipantColorIndex } = useParticipants();
  const { stood, standNumbers, collided, lastStand } = useNunchiGameContext();

  const hasRoster = participants.length > 0;
  const roster = hasRoster
    ? participants.map((participant) => participant.playerName)
    : Array.from(new Set([...stood, ...collided]));

  const getState = (name: string): NunchiPersonState => {
    if (collided.includes(name)) return 'out';
    if (stood.includes(name)) return 'stood';
    return 'seated';
  };

  const seatedCount = roster.filter((name) => getState(name) === 'seated').length;

  return (
    <S.Crowd>
      <S.Row>
        {roster.map((name) => {
          const state = getState(name);
          const colorIndex = hasRoster ? getParticipantColorIndex(name) : fallbackColorIndex(name);
          const color = colorList[colorIndex] ?? colorList[0];
          const isMe = name === myName;
          const pressedNumber = standNumbers[name];
          return (
            <S.Person key={name}>
              <S.IconSlot $state={state} $isMe={isMe}>
                {state === 'stood' && (
                  <S.Badge $color={color} $justStood={lastStand?.name === name}>
                    {pressedNumber ?? <span aria-hidden="true">•</span>}
                  </S.Badge>
                )}
                <PlayerIcon color={color} />
              </S.IconSlot>
              <S.Name $state={state}>
                <Description color={nameColor(state, isMe)}>{name}</Description>
              </S.Name>
            </S.Person>
          );
        })}
      </S.Row>
      {hasRoster && (
        <S.Remaining>
          남은 사람 <S.RemainingCount>{seatedCount}</S.RemainingCount>명
        </S.Remaining>
      )}
    </S.Crowd>
  );
};

// props 가 없고 컨텍스트(WS 메시지)에만 의존한다. 부모 NunchiStage 가 카운트다운으로 매 프레임
// 리렌더되지만 memo 로 그 churn 을 끊어, 군중(최대 9 아바타)은 WS 갱신 시에만 다시 그린다.
export default memo(NunchiCrowd);
