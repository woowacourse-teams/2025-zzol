import { MINI_GAME_NAME_MAP, type MiniGameType } from '@/types/miniGame/common';
import { useState } from 'react';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import useMutation from '@/apis/rest/useMutation';
import BackButton from '@/components/@common/BackButton/BackButton';
import Button from '@/components/@common/Button/Button';
import Headline4 from '@/components/@common/Headline4/Headline4';
import * as S from './SuggestionTab.styled';

type SuggestionCategory = 'BUG' | 'SUGGESTION' | 'GAME_REQUEST' | 'OTHER';
type SuggestionStep = 'category' | 'game-select' | 'form' | 'success';

const CATEGORIES: { key: SuggestionCategory; label: string; icon: string }[] = [
  { key: 'BUG', label: '버그 신고', icon: '🐛' },
  { key: 'SUGGESTION', label: '건의사항', icon: '💡' },
  { key: 'GAME_REQUEST', label: '게임 추가', icon: '🕹️' },
  { key: 'OTHER', label: '기타', icon: '🔧' },
];

const GAME_OPTIONS = Object.entries(MINI_GAME_NAME_MAP) as [MiniGameType, string][];

type ReportBody = {
  category: SuggestionCategory;
  gameType: MiniGameType | null;
  joinCode: string | null;
  content: string;
};

type Props = {
  onBackToMenu?: () => void;
};

const SuggestionTab = ({ onBackToMenu }: Props) => {
  const [step, setStep] = useState<SuggestionStep>('category');
  const [category, setCategory] = useState<SuggestionCategory | null>(null);
  const [gameType, setGameType] = useState<MiniGameType | null>(null);
  const [content, setContent] = useState('');

  const { mutate: submitReport, loading } = useMutation<void, ReportBody>({
    endpoint: '/reports',
    method: 'POST',
    onSuccess: () => setStep('success'),
    errorDisplayMode: 'toast',
  });

  const handleReset = () => {
    setStep('category');
    setCategory(null);
    setGameType(null);
    setContent('');
  };

  const handleCategorySelect = (selected: SuggestionCategory) => {
    setCategory(selected);
    if (selected === 'BUG') setStep('game-select');
    else setStep('form');
  };

  const handleGameSelect = (selected: MiniGameType) => {
    setGameType(selected);
    setStep('form');
  };

  const handleSubmit = () => {
    if (!category) return;
    const lastJoinCode = storageManager.getItem(STORAGE_KEYS.LAST_JOIN_CODE, 'localStorage');
    submitReport({
      category,
      gameType: category === 'BUG' ? gameType : null,
      joinCode: category === 'BUG' ? lastJoinCode : null,
      content,
    });
  };

  const handleBack = () => {
    if (step === 'category') onBackToMenu?.();
    else if (step === 'form' && category === 'BUG') setStep('game-select');
    else handleReset();
  };

  const getFormLabel = () => {
    if (category === 'BUG') {
      return gameType ? `${MINI_GAME_NAME_MAP[gameType]} 버그 내용` : '버그 내용';
    }
    if (category === 'GAME_REQUEST') return '어떤 게임을 원하시나요?';
    return '어떤 내용인가요?';
  };

  const showBackButton = step !== 'success';

  return (
    <S.Container>
      {showBackButton && (
        <BackButton onClick={handleBack} text="돌아가기" />
      )}
      <S.CenterWrapper>
        {step === 'category' && (
          <>
            <Headline4>무엇을 알려주실건가요?</Headline4>
            <S.ChipGrid>
              {CATEGORIES.map(({ key, label, icon }) => (
                <S.CategoryChip key={key} onClick={() => handleCategorySelect(key)}>
                  <S.ChipIcon>{icon}</S.ChipIcon>
                  <S.ChipLabel>{label}</S.ChipLabel>
                </S.CategoryChip>
              ))}
            </S.ChipGrid>
          </>
        )}

        {step === 'game-select' && (
          <>
            <Headline4>어떤 게임에서 발생했나요?</Headline4>
            <S.ChipGrid>
              {GAME_OPTIONS.map(([key, name]) => (
                <S.CategoryChip key={key} onClick={() => handleGameSelect(key)}>
                  <S.ChipLabel>{name}</S.ChipLabel>
                </S.CategoryChip>
              ))}
              <S.CategoryChip $fullWidth onClick={() => { setGameType(null); setStep('form'); }}>
                <S.ChipLabel>게임 외 (로비/룰렛)</S.ChipLabel>
              </S.CategoryChip>
            </S.ChipGrid>
          </>
        )}

        {step === 'form' && (
          <>
            <Headline4>{getFormLabel()}</Headline4>
            <S.Textarea
              placeholder="자유롭게 작성해주세요 (최대 200자)"
              maxLength={200}
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
            <S.CharCount>{content.length}/200</S.CharCount>
            <Button
              variant={content.trim().length === 0 || loading ? 'disabled' : 'primary'}
              onClick={handleSubmit}
            >
              {loading ? '전송 중...' : '제출하기'}
            </Button>
          </>
        )}

        {step === 'success' && (
          <>
            <S.SuccessIcon>✅</S.SuccessIcon>
            <S.SuccessTitle>전달되었습니다!</S.SuccessTitle>
            <S.SuccessDesc>소중한 의견 감사해요.</S.SuccessDesc>
            <S.ResetButton onClick={handleReset}>다시 작성하기</S.ResetButton>
          </>
        )}
      </S.CenterWrapper>
    </S.Container>
  );
};

export default SuggestionTab;
