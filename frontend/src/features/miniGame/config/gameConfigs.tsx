import CardGameDescription1 from '@/assets/card_game_desc1.svg';
import CardGameDescription2 from '@/assets/card_game_desc2.svg';
import RacingGameDescription1 from '@/assets/racing_game_desc1.svg';
import RacingGameDescription2 from '@/assets/racing_game_desc2.svg';
import BlockStackingDescription1 from '@/assets/block_stacking_desc1.svg';
import BlockStackingDescription2 from '@/assets/block_stacking_desc2.svg';
import NunchiGameDescription1 from '@/assets/nunchi_game_desc1.svg';
import NunchiGameDescription2 from '@/assets/nunchi-game-icon.svg';
import CardGameProvider from '@/contexts/CardGame/CardGameProvider';
import RacingGameProvider from '@/contexts/RacingGame/RacingGameProvider';
import SpeedTouchGameProvider from '@/contexts/SpeedTouchGame/SpeedTouchGameProvider';
import BlindTimerGameProvider from '@/contexts/BlindTimerGame/BlindTimerGameProvider';
import BlockStackingGameProvider from '@/contexts/BlockStackingGame/BlockStackingGameProvider';
import LadderGameProvider from '@/contexts/LadderGame/LadderGameProvider';
import { MiniGameType } from '@/types/miniGame/common';
import { ComponentType, PropsWithChildren } from 'react';
import CardGameReadyPage from '../cardGame/pages/CardGameReadyPage';
import RacingGameReadyPage from '../racingGame/pages/RacingGameReadyPage';
import SpeedTouchGameReadyPage from '../speedTouchGame/pages/SpeedTouchGameReadyPage';
import BlindTimerGameReadyPage from '../blindTimerGame/pages/BlindTimerGameReadyPage';
import CardGamePlayPage from '../cardGame/pages/CardGamePlayPage';
import RacingGamePlayPage from '../racingGame/pages/RacingGamePlayPage';
import SpeedTouchGamePlayPage from '../speedTouchGame/pages/SpeedTouchGamePlayPage';
import BlindTimerGamePlayPage from '../blindTimerGame/pages/BlindTimerGamePlayPage';
import BlockStackingGameReadyPage from '../blockStackingGame/pages/BlockStackingGameReadyPage';
import BlockStackingGamePlayPage from '../blockStackingGame/pages/BlockStackingGamePlayPage';
import LadderGameDescription1 from '@/assets/ladder_game_desc1.svg';
import LadderGameDescription2 from '@/assets/ladder_game_desc2.svg';
import LadderGameReadyPage from '../ladderGame/pages/LadderGameReadyPage';
import LadderGamePlayPage from '../ladderGame/pages/LadderGamePlayPage';
import NunchiGameProvider from '@/contexts/NunchiGame/NunchiGameProvider';
import NunchiGameReadyPage from '../nunchiGame/pages/NunchiGameReadyPage';
import NunchiGamePlayPage from '../nunchiGame/pages/NunchiGamePlayPage';
import NunchiGameResultContent from '../nunchiGame/pages/NunchiGameResultPage';

export type SlideConfig = {
  textLines: string[];
  imageSrc?: string;
  className: string;
};

export type GameConfig = {
  Provider: ComponentType<PropsWithChildren>;
  ReadyPage: ComponentType;
  slides: SlideConfig[];
  PlayPage: ComponentType;
  /**
   * 결과 화면의 **본문(Layout.Content) 슬롯만** 교체하는 게임 전용 결과 컴포넌트(선택).
   * 미지정 시 공유 ScoreBoardResultList(rank+score 나열)가 렌더된다.
   * 배너·룰렛 진행 버튼·룰렛 구독은 공유 MiniGameResultPage 가 유지하므로
   * 게임 전용 결과 뷰가 룰렛 흐름을 깨지 않는다(nunchi 3계층 결과 — ADR-0031 N7).
   */
  ResultContent?: ComponentType;
};

export const GAME_CONFIGS: Record<MiniGameType, GameConfig> = {
  CARD_GAME: {
    Provider: CardGameProvider,
    ReadyPage: CardGameReadyPage,
    slides: [
      {
        textLines: ['각 라운드마다', '카드 1장을 선택하세요'],
        imageSrc: CardGameDescription1,
        className: 'slide-first',
      },
      {
        textLines: ['합산된 값으로', '등수가 결정됩니다'],
        imageSrc: CardGameDescription2,
        className: 'slide-second',
      },
    ],
    PlayPage: CardGamePlayPage,
  },
  RACING_GAME: {
    Provider: RacingGameProvider,
    ReadyPage: RacingGameReadyPage,
    slides: [
      {
        textLines: ['빠르게 터치하세요!'],
        imageSrc: RacingGameDescription1,
        className: 'slide-first',
      },
      {
        textLines: ['먼저 도착한 순으로', '등수가 결정됩니다'],
        imageSrc: RacingGameDescription2,
        className: 'slide-second',
      },
    ],
    PlayPage: RacingGamePlayPage,
  },
  SPEED_TOUCH: {
    Provider: SpeedTouchGameProvider,
    ReadyPage: SpeedTouchGameReadyPage,
    slides: [
      {
        textLines: ['1부터 25까지', '순서대로 터치하세요!'],
        className: 'slide-first',
      },
      {
        textLines: ['가장 빠르게 완주한 순으로', '등수가 결정됩니다'],
        className: 'slide-second',
      },
    ],
    PlayPage: SpeedTouchGamePlayPage,
  },
  BLIND_TIMER: {
    Provider: BlindTimerGameProvider,
    ReadyPage: BlindTimerGameReadyPage,
    slides: [
      {
        textLines: ['목표 시간이 주어지면', '타이머를 보며 감을 잡으세요'],
        className: 'slide-first',
      },
      {
        textLines: ['3초 후 화면이 가려지면', '감각만으로 STOP!'],
        className: 'slide-second',
      },
    ],
    PlayPage: BlindTimerGamePlayPage,
  },
  BLOCK_STACKING: {
    Provider: BlockStackingGameProvider,
    ReadyPage: BlockStackingGameReadyPage,
    slides: [
      {
        textLines: ['블록이 좌우로 움직입니다', '화면을 탭해서 블록을 쌓으세요!'],
        imageSrc: BlockStackingDescription1,
        className: 'slide-first',
      },
      {
        textLines: ['많이 쌓을수록', '당첨 확률이 올라갑니다'],
        imageSrc: BlockStackingDescription2,
        className: 'slide-second',
      },
    ],
    PlayPage: BlockStackingGamePlayPage,
  },
  LADDER_GAME: {
    Provider: LadderGameProvider,
    ReadyPage: LadderGameReadyPage,
    slides: [
      {
        textLines: ['사다리를 타서', '순위를 결정합니다'],
        imageSrc: LadderGameDescription1,
        className: 'slide-first',
      },
      {
        textLines: ['기둥 사이를 터치해서', '선을 하나 그을 수 있어요'],
        imageSrc: LadderGameDescription2,
        className: 'slide-second',
      },
    ],
    PlayPage: LadderGamePlayPage,
  },
  NUNCHI_GAME: {
    Provider: NunchiGameProvider,
    ReadyPage: NunchiGameReadyPage,
    slides: [
      {
        textLines: ['눈치껏 한 명씩 누르면', '누른 숫자가 그대로 내 순위!'],
        imageSrc: NunchiGameDescription1,
        className: 'slide-first',
      },
      {
        textLines: ['여럿이 동시에 누르면', '꼴찌 순위로 밀려요'],
        imageSrc: NunchiGameDescription2,
        className: 'slide-second',
      },
    ],
    PlayPage: NunchiGamePlayPage,
    ResultContent: NunchiGameResultContent,
  },
};
