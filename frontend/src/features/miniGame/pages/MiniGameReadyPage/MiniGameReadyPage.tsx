import { MiniGameType } from '@/types/miniGame/common';
import { useParams } from 'react-router-dom';
import { GAME_CONFIGS } from '../../config/gameConfigs';

const MiniGameReadyPage = () => {
  const { miniGameType } = useParams();

  if (!miniGameType || !(miniGameType in GAME_CONFIGS)) {
    return (
      <div>
        <h1>잘못된 미니게임입니다.</h1>
        <p>지원하지 않는 미니게임 타입: {miniGameType}</p>
      </div>
    );
  }

  const ReadyPageComponent = GAME_CONFIGS[miniGameType as MiniGameType].ReadyPage;

  return <ReadyPageComponent />;
};

export default MiniGameReadyPage;
