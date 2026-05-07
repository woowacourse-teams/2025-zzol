import { useEffect, useRef, useState } from 'react';
import * as S from './LowestProbabilitySlide.styled';

type Player = { nickname: string; userCode: string };

type Props = {
  players: Player[];
  probability: number;
};

const LowestProbabilitySlide = ({ players, probability }: Props) => {
  const [displayPct, setDisplayPct] = useState(100.0);
  const rafRef = useRef<number | null>(null);

  useEffect(() => {
    if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);

    if (probability === 0) {
      setDisplayPct(100.0);
      return;
    }

    const target = Math.min(probability, 100);
    const duration = 900;
    const start = performance.now();

    const tick = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      if (progress >= 1) {
        setDisplayPct(target);
        return;
      }
      const eased = 1 - Math.pow(1 - progress, 5);
      setDisplayPct(Math.round(100 - (100 - target) * eased));
      rafRef.current = requestAnimationFrame(tick);
    };

    rafRef.current = requestAnimationFrame(tick);
    return () => {
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
    };
  }, [probability]);

  const pct = Number.isInteger(displayPct) ? String(displayPct) : displayPct.toFixed(1);

  return (
    <S.Card>
      <S.Header>
        <S.CardTitle>최저 확률 당첨</S.CardTitle>
        <S.Sub>이번달 가장 짜릿한 역전</S.Sub>
      </S.Header>
      {players.length === 0 ? (
        <S.Empty>아직 당첨자가 없어요</S.Empty>
      ) : (
        <S.Content>
          <S.ProbSection>
            <S.BigProb>{pct}%</S.BigProb>
            <S.ProbLabel>당첨 확률</S.ProbLabel>
          </S.ProbSection>
          <S.VerticalDivider />
          <S.WinnerSection>
            <S.WinnerLabel>
              {players.length > 1 ? `${players.length}명 공동 당첨` : '당첨자'}
            </S.WinnerLabel>
            {players.length === 1 ? (
              <S.WinnerRow>
                <S.WinnerName>{players[0].nickname}</S.WinnerName>
                <S.WinnerCode>#{players[0].userCode}</S.WinnerCode>
              </S.WinnerRow>
            ) : (
              <S.MultiWinnerList>
                {players.map((p) => (
                  <S.WinnerRow key={p.userCode}>
                    <S.WinnerName>{p.nickname}</S.WinnerName>
                    <S.WinnerCode>#{p.userCode}</S.WinnerCode>
                  </S.WinnerRow>
                ))}
              </S.MultiWinnerList>
            )}
          </S.WinnerSection>
        </S.Content>
      )}
    </S.Card>
  );
};

export default LowestProbabilitySlide;
