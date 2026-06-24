import Skeleton from '@/components/@common/Skeleton/Skeleton';
import * as S from './GameActionButtonSkeleton.styled';

const GameActionButtonSkeleton = () => {
  return (
    <>
      {Array.from({ length: 4 }).map((_, index) => (
        <S.Container key={index}>
          <S.IconWrapper>
            <Skeleton width={50} height={50} borderRadius="30px" />
          </S.IconWrapper>
          <Skeleton width="60%" height={14} />
        </S.Container>
      ))}
    </>
  );
};

export default GameActionButtonSkeleton;
