import { MINI_GAME_NAME_MAP, type MiniGameType } from '@/types/miniGame/common';
import { useState } from 'react';
import { EXTERNAL_LINKS } from '@/constants/external';
import * as S from './SuggestionTab.styled';

type SuggestionCategory = 'BUG' | 'SUGGESTION' | 'GAME_REQUEST' | 'OTHER' | 'INFO';
type SuggestionStep = 'category' | 'game-select' | 'form' | 'info' | 'success';

const CATEGORIES: { key: SuggestionCategory; label: string; icon: string; fullWidth?: boolean }[] = [
  { key: 'BUG', label: '버그 신고', icon: '🐛' },
  { key: 'SUGGESTION', label: '건의사항', icon: '💡' },
  { key: 'GAME_REQUEST', label: '게임 추가', icon: '🕹️' },
  { key: 'OTHER', label: '기타', icon: '🔧' },
  { key: 'INFO', label: '정보', icon: 'ℹ️', fullWidth: true },
];

const GAME_OPTIONS = Object.entries(MINI_GAME_NAME_MAP) as [MiniGameType, string][];

const SuggestionTab = () => {
  const [step, setStep] = useState<SuggestionStep>('category');
  const [category, setCategory] = useState<SuggestionCategory | null>(null);
  const [gameType, setGameType] = useState<MiniGameType | null>(null);
  const [content, setContent] = useState('');

  const handleReset = () => {
    setStep('category');
    setCategory(null);
    setGameType(null);
    setContent('');
  };

  const handleCategorySelect = (selected: SuggestionCategory) => {
    setCategory(selected);
    if (selected === 'INFO') setStep('info');
    else if (selected === 'BUG') setStep('game-select');
    else setStep('form');
  };

  const handleGameSelect = (selected: MiniGameType) => {
    setGameType(selected);
    setStep('form');
  };

  const handleSubmit = () => {
    // TODO: POST /reports API 연결 필요 (docs/api-todo.md 참고)
    console.log({ category, gameType, content });
    setStep('success');
  };

  const handleBack = () => {
    if (step === 'form' && category === 'BUG') setStep('game-select');
    else handleReset();
  };

  const getFormLabel = () => {
    if (category === 'BUG') {
      return gameType ? `${MINI_GAME_NAME_MAP[gameType]} 버그 내용` : '버그 내용';
    }
    if (category === 'GAME_REQUEST') return '어떤 게임을 원하시나요?';
    return '어떤 내용인가요?';
  };

  const showBackButton = step !== 'category' && step !== 'success';

  return (
    <S.Container>
      {showBackButton && (
        <S.BackButton onClick={handleBack}>← 돌아가기</S.BackButton>
      )}
      <S.CenterWrapper>
        {step === 'category' && (
          <>
            <S.StepLabel>무엇을 알려주실건가요?</S.StepLabel>
            <S.ChipGrid>
              {CATEGORIES.map(({ key, label, icon, fullWidth }) => (
                <S.CategoryChip key={key} $fullWidth={fullWidth} onClick={() => handleCategorySelect(key)}>
                  <S.ChipIcon>{icon}</S.ChipIcon>
                  <S.ChipLabel>{label}</S.ChipLabel>
                </S.CategoryChip>
              ))}
            </S.ChipGrid>
          </>
        )}

        {step === 'game-select' && (
          <>
            <S.StepLabel>어떤 게임에서 발생했나요?</S.StepLabel>
            <S.ChipGrid>
              {GAME_OPTIONS.map(([key, name]) => (
                <S.CategoryChip key={key} onClick={() => handleGameSelect(key)}>
                  <S.ChipLabel>{name}</S.ChipLabel>
                </S.CategoryChip>
              ))}
              <S.CategoryChip onClick={() => { setGameType(null); setStep('form'); }}>
                <S.ChipLabel>게임 외 (로비/룰렛)</S.ChipLabel>
              </S.CategoryChip>
            </S.ChipGrid>
          </>
        )}

        {step === 'form' && (
          <>
            <S.StepLabel>{getFormLabel()}</S.StepLabel>
            <S.Textarea
              placeholder="자유롭게 작성해주세요 (최대 200자)"
              maxLength={200}
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
            <S.CharCount>{content.length}/200</S.CharCount>
            <S.SubmitButton disabled={content.trim().length === 0} onClick={handleSubmit}>
              제출하기
            </S.SubmitButton>
          </>
        )}

        {step === 'info' && (
          <S.InfoBox>
            <S.InfoTitle>ZZOL 정보</S.InfoTitle>
            <S.InfoRow>
              <S.InfoLabel>서비스</S.InfoLabel>
              <S.InfoValue>zzol.site</S.InfoValue>
            </S.InfoRow>
            <S.Divider />
            <S.InfoLinkButton href={EXTERNAL_LINKS.GITHUB} target="_blank" rel="noopener noreferrer">
              <span>GitHub 보기</span>
              <span>↗</span>
            </S.InfoLinkButton>
          </S.InfoBox>
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
