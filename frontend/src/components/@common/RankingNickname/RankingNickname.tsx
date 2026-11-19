import * as S from './RankingNickname.styled';

type Props = {
  children: string;
  /** 서버가 검열 전이라 원문 대신 문구로 바꾼 닉네임이다(#1906). */
  masked?: boolean;
};

/** 랭킹 닉네임. 검열 처리 중인 닉네임은 연한 글자로 그리고, 색에 기대지 않게 스크린리더 설명을 붙인다. */
const RankingNickname = ({ children, masked = false }: Props) => {
  if (!masked) return <>{children}</>;

  return (
    <S.Pending>
      {children}
      <S.VisuallyHidden>, 검열 처리 중</S.VisuallyHidden>
    </S.Pending>
  );
};

export default RankingNickname;
