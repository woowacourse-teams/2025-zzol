import { MiniGameType } from '@/types/miniGame/common';
import { useParams } from 'react-router-dom';
import { GAME_CONFIGS } from '../../config/gameConfigs';

const MiniGamePlayPage = () => {
  const { miniGameType } = useParams();

  if (!miniGameType || !(miniGameType in GAME_CONFIGS)) {
    /**
     * TODO: NotFoundPage 스타일과 동일하게 가져갈 것
     * TODO: 홈으로 돌아가기 버튼 추가
     */
    return (
      <div>
        <h1>잘못된 미니게임입니다.</h1>
        <p>지원하지 않는 미니게임 타입: {miniGameType}</p>
      </div>
    );
  }

  const GameComponent = GAME_CONFIGS[miniGameType as MiniGameType].PlayPage;

  return <GameComponent />;
};

export default MiniGamePlayPage;
