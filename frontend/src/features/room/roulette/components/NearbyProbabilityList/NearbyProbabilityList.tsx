import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { MAX_NEARBY_PLAYERS, pickNearbyPlayers } from '../../utils/pickNearbyPlayers';
import { ProbabilityHistory } from '@/types/roulette';
import { formatProbability, formatProbabilityChange } from '@/utils/formatProbability';
import * as S from './NearbyProbabilityList.styled';

type Props = {
  isProbabilitiesLoading: boolean;
};

const NearbyProbabilityList = ({ isProbabilitiesLoading }: Props) => {
  const { probabilityHistory } = useProbabilityHistory();
  const { myName } = useIdentifier();

  const nearbyPlayers = pickNearbyPlayers(probabilityHistory.current, myName);
  const myProbabilityChange = getMyProbabilityChange(probabilityHistory, myName);

  // 다 받았는데도 비어 있다면(내가 목록에 없는 경우) 그릴 것이 없으므로 아무것도 내지 않는다.
  if (nearbyPlayers.length === 0 && !isProbabilitiesLoading) return null;

  // 확률이 오기 전에는 빈 행으로 자리를 잡아 둔다. 자리를 안 잡으면 데이터가 도착하는 순간
  // 리스트 높이만큼 휠이 갑자기 줄어든다 (휠이 남는 높이를 전부 쓰기 때문).
  //
  // 높이를 min-height 로 못 박지 않는 이유: 보이는 행 수는 화면 높이에 따라 2~5로 달라진다.
  // 실제와 같은 근접 순위로 빈 행을 그리면 같은 미디어쿼리가 걸려 어떤 화면에서도 높이가 맞는다.
  const placeholderRows = Array.from({ length: MAX_NEARBY_PLAYERS + 1 }, (_, rank) => rank);

  return (
    <S.Container $isLoading={isProbabilitiesLoading}>
      <S.Caption id="nearby-probability-caption">나와 확률이 가까운 참가자</S.Caption>
      <S.List aria-labelledby="nearby-probability-caption">
        {nearbyPlayers.length === 0 &&
          placeholderRows.map((rank) => (
            <S.Row key={`placeholder-${rank}`} $proximityRank={rank} $isMine={false} aria-hidden>
              {/* 실제 행과 같은 요소를 써야 높이가 정확히 같다 */}
              <S.Name $isMine={false}>&nbsp;</S.Name>
            </S.Row>
          ))}
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
                  {formatProbabilityChange(myProbabilityChange)}
                </S.Change>
              )}
              <S.Probability>{formatProbability(probability)}</S.Probability>
            </S.Row>
          );
        })}
      </S.List>
    </S.Container>
  );
};

export default NearbyProbabilityList;

const getMyProbability = (players: ProbabilityHistory['current'], myName: string) =>
  players.find((player) => player.playerName === myName)?.probability ?? 0;

const getMyProbabilityChange = (probabilityHistory: ProbabilityHistory, myName: string) =>
  getMyProbability(probabilityHistory.current, myName) -
  getMyProbability(probabilityHistory.prev, myName);
