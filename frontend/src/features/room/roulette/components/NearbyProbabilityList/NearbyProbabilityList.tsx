import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { pickNearbyPlayers } from '@/features/roulette/utils/pickNearbyPlayers';
import { ProbabilityHistory } from '@/types/roulette';
import * as S from './NearbyProbabilityList.styled';

const percentFormat = new Intl.NumberFormat('ko-KR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

type Props = {
  isProbabilitiesLoading: boolean;
};

const NearbyProbabilityList = ({ isProbabilitiesLoading }: Props) => {
  const { probabilityHistory } = useProbabilityHistory();
  const { myName } = useIdentifier();

  const nearbyPlayers = pickNearbyPlayers(probabilityHistory.current, myName);
  const myProbabilityChange = getMyProbabilityChange(probabilityHistory, myName);

  if (nearbyPlayers.length === 0) return null;

  return (
    <S.Container $isLoading={isProbabilitiesLoading}>
      <S.Caption id="nearby-probability-caption">나와 확률이 가까운 참가자</S.Caption>
      <S.List aria-labelledby="nearby-probability-caption">
        {nearbyPlayers.map(({ playerName, probability, playerColor, proximityRank }) => {
          const isMine = playerName === myName;

          return (
            <S.Row key={playerName} $proximityRank={proximityRank} $isMine={isMine}>
              <S.Dot $color={playerColor} />
              <S.Name $isMine={isMine}>{playerName}</S.Name>
              {isMine && (
                <S.Change $isPositive={myProbabilityChange >= 0}>
                  <ScreenReaderOnly>{myProbabilityChange >= 0 ? '증가' : '감소'}</ScreenReaderOnly>
                  {formatChange(myProbabilityChange)}
                </S.Change>
              )}
              <S.Probability>{percentFormat.format(probability)}%</S.Probability>
            </S.Row>
          );
        })}
      </S.List>
    </S.Container>
  );
};

export default NearbyProbabilityList;

const formatChange = (change: number) =>
  `${change >= 0 ? '+' : ''}${percentFormat.format(change)}%`;

const getMyProbability = (players: ProbabilityHistory['current'], myName: string) =>
  players.find((player) => player.playerName === myName)?.probability ?? 0;

const getMyProbabilityChange = (probabilityHistory: ProbabilityHistory, myName: string) =>
  getMyProbability(probabilityHistory.current, myName) -
  getMyProbability(probabilityHistory.prev, myName);
