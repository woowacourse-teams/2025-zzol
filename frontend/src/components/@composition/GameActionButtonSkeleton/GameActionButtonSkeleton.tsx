import Skeleton from '@/components/@common/Skeleton/Skeleton';
import * as S from './GameActionButtonSkeleton.styled';

const GameActionButtonSkeleton = () => {
  return (
    <>
      {Array.from({ length: 2 }).map((_, index) => (
        <S.Container key={index}>
          <S.ContentWrapper>
            <Skeleton width="20%" height={24} />
            <S.DescriptionWrapper>
              <Skeleton width="70%" height={14} />
              <Skeleton width="60%" height={14} />
            </S.DescriptionWrapper>
          </S.ContentWrapper>
          <S.IconWrapper>
            <Skeleton width={50} height={50} borderRadius="30px" />
          </S.IconWrapper>
        </S.Container>
      ))}
    </>
  );
};

export default GameActionButtonSkeleton;
