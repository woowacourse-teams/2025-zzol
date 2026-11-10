import * as S from './RankingNickname.styled';

type Props = {
  children: string;
  /** 서버가 검열 전이라 원문 대신 문구로 바꾼 닉네임이다(#1906). */
  masked?: boolean;
  /** light는 밝은 배경, dark는 짙은 색 배경 위에 그릴 때 쓴다. */
  tone?: S.Tone;
};

/** 랭킹 닉네임. 검열 처리 중인 닉네임은 색에 기대지 않고 흐르는 배경과 스크린리더 설명으로 구분한다. */
const RankingNickname = ({ children, masked = false, tone = 'light' }: Props) => {
  if (!masked) return <>{children}</>;

  return (
    <S.Pending $tone={tone}>
      {children}
      <S.VisuallyHidden>, 검열 처리 중</S.VisuallyHidden>
    </S.Pending>
  );
};

export default RankingNickname;
