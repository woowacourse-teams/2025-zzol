import * as S from './TrackNotice.styled';

/** 서버 목록에서 내 이름을 못 찾았을 때 띄운다. 빈 하늘만 보여 주는 것보다 낫다. */
const TrackNotice = () => {
  return (
    <S.Notice role="status" aria-live="polite">
      <S.Title>참가 정보를 불러오는 중</S.Title>
      <S.Hint>순위와 진행률은 그대로 움직입니다.</S.Hint>
    </S.Notice>
  );
};

export default TrackNotice;
