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

    let cancelled = false;
    const target = Math.min(probability, 100);
    const PHASE1_DURATION = 300;
    const PHASE2_DURATION = 900;
    const start = performance.now();

    const tick = (now: number) => {
      if (cancelled) return;

      const elapsed = now - start;

      if (elapsed >= PHASE1_DURATION + PHASE2_DURATION) {
        setDisplayPct(target);
        return;
      }

      let raw: number;
      if (elapsed < PHASE1_DURATION) {
        const p = elapsed / PHASE1_DURATION;
        raw = 100 - 50 * p;
      } else {
        const p = (elapsed - PHASE1_DURATION) / PHASE2_DURATION;
        const eased = 1 - Math.pow(1 - p, 3);
        raw = 50 - (50 - target) * eased;
      }

      const snapped =
        raw > Math.ceil(target) || Number.isInteger(target)
          ? Math.round(raw)
          : Math.round(raw * 10) / 10;
      setDisplayPct(snapped);
      rafRef.current = requestAnimationFrame(tick);
    };

    rafRef.current = requestAnimationFrame(tick);
    return () => {
      cancelled = true;
      if (rafRef.current !== null) {
        cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
    };
  }, [probability]);

  const pct = displayPct.toFixed(1).replace(/\.0$/, '');

  return (
    <S.Card>
      <S.Header>
        <S.CardTitle>최저 확률 당첨</S.CardTitle>
        <S.Sub>이번 달 가장 행복한 당첨자~</S.Sub>
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
