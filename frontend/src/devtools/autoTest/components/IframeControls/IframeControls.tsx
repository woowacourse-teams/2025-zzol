import { MINI_GAME_NAME_MAP, type MiniGameType } from '@/types/miniGame/common';
import * as S from './IframeControls.styled';

type PanelControls = {
  open: boolean;
  onToggleOpen: () => void;
};

type GameSelectionControls = {
  isExpanded: boolean;
  gameSequence: MiniGameType[];
  availableGames: MiniGameType[];
  onToggleExpanded: () => void;
  onToggleGame: (game: MiniGameType) => void;
};

type TestControls = {
  isRunning: boolean;
  isPaused: boolean;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onStop: () => void;
};

export type IframeControlsProps = {
  panel: PanelControls;
  gameSelection: GameSelectionControls;
  test: TestControls;
};

const IframeControls = ({ panel, gameSelection, test }: IframeControlsProps) => {
  const { open, onToggleOpen } = panel;
  const { isExpanded, gameSequence, availableGames, onToggleExpanded, onToggleGame } =
    gameSelection;
  const { isRunning, isPaused, onStart, onPause, onResume, onStop } = test;

  return (
    <S.ToggleBar>
      <S.ToggleButton type="button" onClick={onToggleOpen}>
        {open ? 'Hide iframes' : 'Show iframes'}
      </S.ToggleButton>
      {open && (
        <>
          <S.GameSelectionContainer $isExpanded={isExpanded}>
            <S.GameSelectionLabel type="button" onClick={onToggleExpanded}>
              게임 선택
            </S.GameSelectionLabel>
            {isExpanded && (
              <S.GameSelectionButtons>
                {availableGames.map((game) => {
                  const isSelected = gameSequence.includes(game);
                  const order = gameSequence.indexOf(game) + 1;

                  return (
                    <S.GameSelectionButton
                      key={game}
                      type="button"
                      $selected={isSelected}
                      disabled={isRunning}
                      onClick={() => onToggleGame(game)}
                    >
                      {MINI_GAME_NAME_MAP[game]}
                      <S.GameOrderBadge $visible={isSelected && order > 0}>
                        {order > 0 ? order : ''}
                      </S.GameOrderBadge>
                    </S.GameSelectionButton>
                  );
                })}
              </S.GameSelectionButtons>
            )}
          </S.GameSelectionContainer>
          <S.PlayButton type="button" onClick={onStart} disabled={isRunning}>
            {isRunning ? '테스트 실행 중...' : '재생'}
          </S.PlayButton>
          {isRunning && !isPaused && (
            <S.PauseButton type="button" onClick={onPause}>
              일시 중지
            </S.PauseButton>
          )}
          {isRunning && isPaused && (
            <S.ResumeButton type="button" onClick={onResume}>
              재개
            </S.ResumeButton>
          )}
          {isRunning && (
            <S.StopButton type="button" onClick={onStop}>
              테스트 중단
            </S.StopButton>
          )}
        </>
      )}
    </S.ToggleBar>
  );
};

export default IframeControls;
