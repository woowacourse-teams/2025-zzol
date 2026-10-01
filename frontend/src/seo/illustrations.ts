import BlockStacking1 from '@/assets/block_stacking_desc1.svg';
import BlockStacking2 from '@/assets/block_stacking_desc2.svg';
import CardGame1 from '@/assets/card_game_desc1.svg';
import CardGame2 from '@/assets/card_game_desc2.svg';
import LadderGame1 from '@/assets/ladder_game_desc1.svg';
import LadderGame2 from '@/assets/ladder_game_desc2.svg';
import NunchiGame1 from '@/assets/nunchi_game_desc1.svg';
import RacingGame1 from '@/assets/racing_game_desc1.svg';
import RacingGame2 from '@/assets/racing_game_desc2.svg';
import type { MiniGameType } from '@/types/miniGame/common';

/**
 * 인게임 설명 카루셀이 쓰는 그림을 SEO 상세 페이지에서도 쓴다.
 * `gameConfigs`에서 가져오지 않는 이유는 그걸 임포트하면 게임 번들 전체가 딸려오기 때문이다.
 * 그림이 없는 게임(1 to 25·뇌피셜 초시계)은 키를 두지 않는다 — 아이콘 히어로로 폴백한다.
 */
export const MINI_GAME_ILLUSTRATION_MAP: Partial<Record<MiniGameType, string[]>> = {
  CARD_GAME: [CardGame1, CardGame2],
  RACING_GAME: [RacingGame1, RacingGame2],
  BLOCK_STACKING: [BlockStacking1, BlockStacking2],
  LADDER_GAME: [LadderGame1, LadderGame2],
  NUNCHI_GAME: [NunchiGame1],
};
