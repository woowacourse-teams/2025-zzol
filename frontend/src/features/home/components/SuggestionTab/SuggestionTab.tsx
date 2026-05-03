import { MINI_GAME_NAME_MAP, type MiniGameType } from '@/types/miniGame/common';
import { useState } from 'react';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import useMutation from '@/apis/rest/useMutation';
import Button from '@/components/@common/Button/Button';
import * as S from './SuggestionTab.styled';
import BugIcon from '@/assets/bug-icon.svg';
import SuggestionIcon from '@/assets/suggestion-icon.svg';
import GameRequestIcon from '@/assets/game-request-icon.svg';
import OtherIcon from '@/assets/other-icon.svg';
import CheckIcon from '@/assets/check-icon.svg';

type SuggestionCategory = 'BUG' | 'SUGGESTION' | 'GAME_REQUEST' | 'OTHER';

const CATEGORIES: { key: SuggestionCategory; label: string; icon: string }[] = [
  { key: 'BUG', label: '버그 신고', icon: BugIcon },
  { key: 'SUGGESTION', label: '건의사항', icon: SuggestionIcon },
  { key: 'GAME_REQUEST', label: '게임 추가', icon: GameRequestIcon },
  { key: 'OTHER', label: '기타', icon: OtherIcon },
];

const GAME_OPTIONS = Object.entries(MINI_GAME_NAME_MAP) as [MiniGameType, string][];

type ReportBody = {
  category: SuggestionCategory;
  gameType: MiniGameType | null;
  joinCode: string | null;
  content: string;
};

const SuggestionTab = () => {
  const [category, setCategory] = useState<SuggestionCategory>('BUG');
  const [gameType, setGameType] = useState<MiniGameType | null>(null);
  const [isOtherGame, setIsOtherGame] = useState(false);
  const [content, setContent] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const { mutate: submitReport, loading } = useMutation<void, ReportBody>({
    endpoint: '/reports',
    method: 'POST',
    onSuccess: () => setSubmitted(true),
    errorDisplayMode: 'toast',
  });

  const handleCategoryChange = (cat: SuggestionCategory) => {
    setCategory(cat);
    setGameType(null);
    setIsOtherGame(false);
  };

  const handleGamePillClick = (key: MiniGameType) => {
    setGameType((prev) => (prev === key ? null : key));
    setIsOtherGame(false);
  };

  const handleOtherGameClick = () => {
    setGameType(null);
    setIsOtherGame((prev) => !prev);
  };

  const handleSubmit = () => {
    const lastJoinCode = storageManager.getItem(STORAGE_KEYS.LAST_JOIN_CODE, 'localStorage');
    submitReport({
      category,
      gameType: category === 'BUG' ? gameType : null,
      joinCode: category === 'BUG' ? lastJoinCode : null,
      content,
    });
  };

  const handleReset = () => {
    setContent('');
    setGameType(null);
    setIsOtherGame(false);
    setSubmitted(false);
  };

  const getFormLabel = () => {
    if (category === 'BUG') {
      return gameType ? `${MINI_GAME_NAME_MAP[gameType]} 버그 내용` : '버그 내용';
    }
    if (category === 'GAME_REQUEST') return '어떤 게임을 원하시나요?';
    return '어떤 내용인가요?';
  };

  if (submitted) {
    return (
      <S.SuccessContainer>
        <S.SuccessIconWrap>
          <img src={CheckIcon} alt="성공" />
        </S.SuccessIconWrap>
        <S.SuccessTitle>전달되었습니다!</S.SuccessTitle>
        <S.SuccessDesc>소중한 의견 감사해요.</S.SuccessDesc>
        <S.ResetButton onClick={handleReset}>다시 보내기</S.ResetButton>
      </S.SuccessContainer>
    );
  }

  return (
    <S.Container>
      <S.CategoryGrid>
        {CATEGORIES.map(({ key, label, icon }) => (
          <S.CategoryTab
            key={key}
            $active={category === key}
            onClick={() => handleCategoryChange(key)}
          >
            <S.TabIcon src={icon} alt="" aria-hidden="true" $active={category === key} />
            <S.TabLabel $active={category === key}>{label}</S.TabLabel>
          </S.CategoryTab>
        ))}
      </S.CategoryGrid>

      {category === 'BUG' && (
        <S.GameSection>
          <S.GameSectionLabel>어떤 게임에서 발생했나요? (선택)</S.GameSectionLabel>
          <S.GamePillRow>
            {GAME_OPTIONS.map(([key, name]) => (
              <S.GamePill
                key={key}
                $active={gameType === key}
                onClick={() => handleGamePillClick(key)}
              >
                {name}
              </S.GamePill>
            ))}
            <S.GamePill $active={isOtherGame} onClick={handleOtherGameClick}>
              게임 외
            </S.GamePill>
          </S.GamePillRow>
        </S.GameSection>
      )}

      <S.FormSection>
        <S.FormLabel>{getFormLabel()}</S.FormLabel>
        <S.Textarea
          placeholder="자유롭게 작성해주세요 (최대 200자)"
          maxLength={200}
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
        <S.FormFooter>
          <S.CharCount $warn={content.length > 180}>{content.length}/200</S.CharCount>
        </S.FormFooter>
      </S.FormSection>

      <Button
        variant={content.trim().length === 0 || loading ? 'disabled' : 'primary'}
        onClick={handleSubmit}
      >
        {loading ? '전송 중...' : '제출하기'}
      </Button>
    </S.Container>
  );
};

export default SuggestionTab;
