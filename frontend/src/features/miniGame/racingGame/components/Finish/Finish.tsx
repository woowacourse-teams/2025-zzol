import * as S from './Finish.styled';

/**
 * 결과 화면으로 넘어가기 전 2초 동안 머무는 완주 연출.
 *
 * 순위는 다음 페이지 스코어보드가 보여 주므로 여기서는 완주했다는 순간만 붙잡는다.
 */
const Finish = () => {
  return (
    <S.Container role="status" aria-live="assertive">
      <S.Title>FINISH</S.Title>
      <S.CheckerRule />
    </S.Container>
  );
};

export default Finish;
