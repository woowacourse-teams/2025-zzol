import Skeleton from '@/components/@common/Skeleton/Skeleton';
import * as S from './MiniGameResultSkeleton.styled';

const MiniGameResultSkeleton = () => {
  return (
    <>
      {Array.from({ length: 4 }).map((_, index) => (
        <S.PlayerCardWrapper key={index}>
          <Skeleton width={35} height={35} borderRadius="12px" />
          <S.PlayerInfoWrapper>
            <Skeleton width={50} height={50} borderRadius="50%" />
            <S.TextWrapper>
              <Skeleton width="40%" height={20} />
            </S.TextWrapper>
          </S.PlayerInfoWrapper>
          <S.ScoreWrapper>
            <Skeleton width={60} height={20} />
          </S.ScoreWrapper>
        </S.PlayerCardWrapper>
      ))}
    </>
  );
};

export default MiniGameResultSkeleton;
