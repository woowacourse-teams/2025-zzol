import Layout from '@/layouts/Layout';
import { MiniGameType } from '@/types/miniGame/common';
import MiniGameIntroSlide from '../MiniGameIntroSlide/MiniGameIntroSlide';
import { GAME_CONFIGS } from '../../config/gameConfigs';

type Props = {
  gameType: MiniGameType;
};

const GameIntroSlides = ({ gameType }: Props) => {
  const slideConfig = GAME_CONFIGS[gameType].slides;

  return (
    <Layout color="point-400">
      <Layout.Content>
        {slideConfig.map((slide, index) => (
          <MiniGameIntroSlide
            key={`${gameType}-slide-${index}`}
            textLines={slide.textLines}
            imageSrc={slide.imageSrc}
            className={slide.className}
          />
        ))}
      </Layout.Content>
    </Layout>
  );
};

export default GameIntroSlides;
