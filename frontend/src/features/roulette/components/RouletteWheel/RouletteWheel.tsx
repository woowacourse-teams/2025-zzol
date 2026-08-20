import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { theme } from '@/styles/theme';
import { PlayerProbability, RouletteSector } from '@/types/roulette';
import { memo } from 'react';
import { WHEEL_CONFIG } from '../../constants/config';
import { convertProbabilitiesToAngles } from '../../utils';
import RouletteSlice from '../RouletteSlice/RouletteSlice';
import * as S from './RouletteWheel.styled';

type Props =
  | {
      sectors: RouletteSector[];
      playerProbabilities?: never;
      isSpinStarted?: boolean;
      finalRotation?: number;
    }
  | {
      sectors?: never;
      playerProbabilities: PlayerProbability[];
      isSpinStarted?: boolean;
      finalRotation?: number;
    };

const DIVIDER_WIDTH = 2;

const RouletteWheel = ({
  sectors,
  playerProbabilities,
  isSpinStarted = false,
  finalRotation = 0,
}: Props) => {
  const { myName } = useIdentifier();

  const playersWithAngles = sectors || convertProbabilitiesToAngles(playerProbabilities);

  const sortedPlayers = [...playersWithAngles].sort((a, b) => {
    if (a.playerName === myName) return 1;
    if (b.playerName === myName) return -1;
    return 0;
  });

  return (
    <S.Container>
      <Pin />
      <S.Wrapper $isSpinStarted={isSpinStarted} $finalRotation={finalRotation}>
        {/*
          role="img" 를 주면 SVG 전체가 이미지 하나로 접히면서 조각 이름이 접근성 트리에서
          사라진다. 얇은 조각은 이름을 아예 그리지 않으므로, 낭독은 SVG 가 아니라 화면에 함께
          놓인 확률 목록이 맡는다 (대기방은 useRouletteScreenReader, 플레이 화면은 근접 확률 리스트).
        */}
        <svg viewBox={`0 0 ${WHEEL_CONFIG.SIZE} ${WHEEL_CONFIG.SIZE}`} aria-hidden="true">
          {sortedPlayers.map((player) => {
            const isMine = player.playerName === myName;

            return (
              <RouletteSlice
                key={player.playerName}
                player={player}
                // 조각마다 흰 구분선을 둔다. 없으면 비슷한 색이 이웃할 때 경계가 사라진다.
                strokeColor={isMine ? theme.color.yellow : theme.color.white}
                strokeWidth={isMine ? WHEEL_CONFIG.STROKE_WIDTH : DIVIDER_WIDTH}
              />
            );
          })}
        </svg>
      </S.Wrapper>
    </S.Container>
  );
};

export default RouletteWheel;

const Pin = memo(() => <S.Pin />);
Pin.displayName = 'Pin';
