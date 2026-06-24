import { useMemo, useRef } from 'react';
import RankItem from '../RankItem/RankItem';
import * as S from './RacingRanks.styled';

type Player = {
  playerName: string;
  position: number;
};

type Props = {
  players: Player[];
  myName: string;
  endDistance: number;
};

const RacingRanks = ({ players, myName, endDistance }: Props) => {
  const finishOrderRef = useRef<Player[]>([]);

  const rankedPlayers = useMemo(() => {
    const finishOrder = finishOrderRef.current;

    players.forEach(({ playerName, position }) => {
      if (
        position >= endDistance &&
        !finishOrder.some((player) => player.playerName === playerName)
      ) {
        finishOrder.push({ playerName, position });
      }
    });

    const unFinishedSortedPlayers = players
      .filter((player) => !finishOrder.some((p) => p.playerName === player.playerName))
      .sort((a, b) => b.position - a.position);

    return [...finishOrder, ...unFinishedSortedPlayers];
  }, [players, endDistance]);

  return (
    <S.Container>
      <S.RankList>
        {rankedPlayers.map((player, index) => {
          const isMe = player.playerName === myName;

          return (
            <RankItem
              key={player.playerName}
              playerName={player.playerName}
              rank={index + 1}
              isMe={isMe}
              isFixed={player.position >= endDistance}
            />
          );
        })}
      </S.RankList>
    </S.Container>
  );
};

export default RacingRanks;
