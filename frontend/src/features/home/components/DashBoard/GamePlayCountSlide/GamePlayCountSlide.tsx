import type { GamePlayCount } from '@/types/dashBoard';
import { MINI_GAME_NAME_MAP, type MiniGameType } from '@/types/miniGame/common';
import * as S from './GamePlayCountSlide.styled';

type Props = {
  games: GamePlayCount[];
};

const GamePlayCountSlide = ({ games }: Props) => {
  const maxCount = Math.max(...games.map((g) => g.playCount), 1);

  return (
    <S.Card>
      <S.CardTitle>미니게임 인기순</S.CardTitle>
      {games.length === 0 ? (
        <S.Empty>아직 플레이 기록이 없어요</S.Empty>
      ) : (
        <S.List>
          {games.map((game, index) => (
            <S.Item key={game.gameType} $index={index}>
              <S.GameRank>{index + 1}</S.GameRank>
              <S.GameInfo>
                <S.GameName>
                  {MINI_GAME_NAME_MAP[game.gameType as MiniGameType] || game.gameType}
                </S.GameName>
                <S.BarTrack>
                  <S.BarFill $ratio={game.playCount / maxCount} />
                </S.BarTrack>
              </S.GameInfo>
              <S.PlayCount>{game.playCount.toLocaleString()}</S.PlayCount>
            </S.Item>
          ))}
        </S.List>
      )}
    </S.Card>
  );
};

export default GamePlayCountSlide;
