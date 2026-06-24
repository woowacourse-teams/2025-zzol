import Button from '@/components/@common/Button/Button';
import * as S from './Pagination.styled';

type Props = {
  currentPage: number;
  totalPages: number;
  onPrevious: () => void;
  onNext: () => void;
};

const Pagination = ({ currentPage, totalPages, onPrevious, onNext }: Props) => {
  return (
    <S.PaginationContainer>
      <S.PaginationButton onClick={onPrevious} disabled={currentPage === 0}>
        &#8249; 이전
      </S.PaginationButton>
      <S.DotsContainer>
        {Array.from({ length: totalPages }, (_, index) => (
          <S.Dot key={index} active={index === currentPage} />
        ))}
      </S.DotsContainer>
      {currentPage + 1 === totalPages ? (
        <Button width="70px" height="small" onClick={onNext}>
          시작하기
        </Button>
      ) : (
        <S.PaginationButton onClick={onNext}>다음 &#8250;</S.PaginationButton>
      )}
    </S.PaginationContainer>
  );
};

export default Pagination;
