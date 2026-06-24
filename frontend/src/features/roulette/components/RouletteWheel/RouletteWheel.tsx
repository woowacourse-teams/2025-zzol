import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
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
        <svg
          width={WHEEL_CONFIG.SIZE}
          height={WHEEL_CONFIG.SIZE}
          viewBox={`0 0 ${WHEEL_CONFIG.SIZE} ${WHEEL_CONFIG.SIZE}`}
        >
          <GlowFilter />
          {sortedPlayers.map((player) => (
            <RouletteSlice
              key={player.playerName}
              player={player}
              strokeColor={player.playerName === myName ? '#FFFF8F' : 'transparent'}
              isGlowing={player.playerName === myName}
            />
          ))}
        </svg>
      </S.Wrapper>
    </S.Container>
  );
};

export default RouletteWheel;

const Pin = memo(() => <S.Pin />);
Pin.displayName = 'Pin';

const GlowFilter = memo(({ id = 'glow' }: { id?: string }) => (
  <defs>
    <filter id={id} x="-50%" y="-50%" width="200%" height="200%">
      <feGaussianBlur stdDeviation="5" result="coloredBlur" />
      <feMerge>
        <feMergeNode in="coloredBlur" />
        <feMergeNode in="SourceGraphic" />
      </feMerge>
    </filter>
  </defs>
));
GlowFilter.displayName = 'GlowFilter';
