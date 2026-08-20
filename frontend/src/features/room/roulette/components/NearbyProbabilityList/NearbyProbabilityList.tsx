import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { pickNearbyPlayers } from '../../utils/pickNearbyPlayers';
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

  // 확률이 오기 전에는 비어 있어도 자리를 잡아 둔다. 여기서 null 을 돌려주면 데이터가 도착하는
  // 순간 리스트 높이만큼 휠이 갑자기 줄어든다 (휠이 남는 높이를 전부 쓰기 때문).
  // 다 받았는데도 비어 있다면(내가 목록에 없는 경우) 그릴 것이 없으므로 아무것도 내지 않는다.
  if (nearbyPlayers.length === 0 && !isProbabilitiesLoading) return null;

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
                // 부호가 증감을 그대로 전달하므로 따로 낭독용 텍스트를 두지 않는다.
                // ScreenReaderOnly 는 div + aria-live 라 span 안에 넣으면 무효 마크업이고,
                // 확률이 갱신될 때마다 "증가"만 문맥 없이 읽힌다.
                <S.Change $isPositive={myProbabilityChange >= 0}>
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
