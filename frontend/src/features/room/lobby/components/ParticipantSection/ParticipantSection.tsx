import Divider from '@/components/@common/Divider/Divider';
import ProgressCounter from '@/components/@common/ProgressCounter/ProgressCounter';
import PlayerCard from '@/components/@composition/PlayerCard/PlayerCard';
import SectionTitle from '@/components/@composition/SectionTitle/SectionTitle';
import { colorList } from '@/constants/color';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { Player } from '@/types/player';
import * as S from './ParticipantSection.styled';

const TOTAL_PARTICIPANTS = 9;

type Props = { participants: Player[] };

export const ParticipantSection = ({ participants }: Props) => {
  const { myName } = useIdentifier();

  const mySelect = participants.filter((participant) => participant.playerName === myName)[0];

  const filteredParticipants = participants.filter(
    (participant) => participant.playerName !== myName
  );

  const myColorIndex =
    participants.find((participant) => participant.playerName === myName)?.colorIndex ?? 0;
  const myColor = colorList[myColorIndex];

  if (!mySelect) {
    return null;
  }

  return (
    <>
      <SectionTitle
        title="참가자"
        description="참가자 목록과 준비상태를 확인할 수 있습니다"
        suffix={
          <ProgressCounter
            current={participants.length}
            total={TOTAL_PARTICIPANTS}
            ariaLabel={`${TOTAL_PARTICIPANTS}명 중 ${participants.length}명 참가중`}
          />
        }
      />
      <PlayerCard
        name={myName}
        playerColor={myColor}
        isReady={mySelect.isReady}
        playerType={mySelect.playerType}
      />
      <Divider />
      <S.ScrollableWrapper>
        {filteredParticipants.length === 0 ? (
          <S.Empty>현재 참여한 인원이 없습니다</S.Empty>
        ) : (
          filteredParticipants.map((participant) => (
            <PlayerCard
              key={participant.playerName}
              name={participant.playerName}
              playerColor={colorList[participant.colorIndex]}
              isReady={participant.isReady}
              playerType={participant.playerType}
            />
          ))
        )}
      </S.ScrollableWrapper>
      <S.BottomGap />
    </>
  );
};
