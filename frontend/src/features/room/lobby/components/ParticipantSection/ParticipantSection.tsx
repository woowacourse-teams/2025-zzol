import Divider from '@/components/@common/Divider/Divider';
import GearIcon from '@/components/@common/GearIcon/GearIcon';
import ProgressCounter from '@/components/@common/ProgressCounter/ProgressCounter';
import PlayerCard from '@/components/@composition/PlayerCard/PlayerCard';
import SectionTitle from '@/components/@composition/SectionTitle/SectionTitle';
import { colorList } from '@/constants/color';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { Player } from '@/types/player';
import { useCallback, useState } from 'react';
import RouletteSettingsModal from '../RouletteSettingsModal/RouletteSettingsModal';
import * as S from './ParticipantSection.styled';

const TOTAL_PARTICIPANTS = 9;

type Props = { participants: Player[] };

export const ParticipantSection = ({ participants }: Props) => {
  const { myName } = useIdentifier();
  const { playerType } = usePlayerType();
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [savedWeight, setSavedWeight] = useState<number | undefined>(undefined);

  const isHost = playerType === 'HOST';

  const handleSettingsOpen = useCallback(() => setIsSettingsOpen(true), []);
  const handleSettingsClose = useCallback(() => setIsSettingsOpen(false), []);
  const handleWeightSave = useCallback((weight: number) => setSavedWeight(weight), []);

  const mySelect = participants.find((participant) => participant.playerName === myName);
  const myColor = mySelect ? colorList[mySelect.colorIndex] : colorList[0];

  const filteredParticipants = participants.filter(
    (participant) => participant.playerName !== myName
  );

  if (!mySelect) {
    return null;
  }

  return (
    <>
      <SectionTitle
        title="참가자"
        description="참가자 목록과 준비상태를 확인할 수 있습니다"
        suffix={
          <S.SuffixRow>
            <ProgressCounter
              current={participants.length}
              total={TOTAL_PARTICIPANTS}
              ariaLabel={`${TOTAL_PARTICIPANTS}명 중 ${participants.length}명 참가중`}
            />
            {isHost && (
              <S.SettingsButton
                type="button"
                aria-label="가중치 설정 변경"
                onClick={handleSettingsOpen}
              >
                <GearIcon size={16} stroke="currentColor" />
                설정
              </S.SettingsButton>
            )}
          </S.SuffixRow>
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
          <S.Empty role="status">현재 참여한 인원이 없습니다</S.Empty>
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

      <RouletteSettingsModal
        isOpen={isSettingsOpen}
        onClose={handleSettingsClose}
        currentWeight={savedWeight}
        onSave={handleWeightSave}
      />
    </>
  );
};
