import type { UseQueryResult } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { ErrorState, Skeleton } from '@/components/ui/EmptyState';

type LoadedProps<T> = {
  query: UseQueryResult<T>;
  /** 로딩 중 자리 표시. 실제 내용과 같은 높이로 주면 데이터가 와도 화면이 안 튄다. */
  skeleton?: ReactNode;
  /**
   * 실패 자리에 붙일 클래스. 카드 밖(격자 칸 등)에 바로 놓일 때 쓴다.
   * 카드 안이면 필요 없다. 카드가 이미 표면과 테두리를 준다.
   */
  errorClassName?: string;
  children: (data: T) => ReactNode;
};

/**
 * 조회 상태 셋(로딩, 실패, 성공)을 한 곳에서 처리한다.
 *
 * <p>이걸 만든 이유는 <b>실패가 조용히 사라지고 있었기 때문</b>이다. 화면마다
 * {@code query.data && (...)} 로만 감싸 두면 요청이 실패했을 때 값 자리에 "-" 가
 * 남는다. 그 "-" 는 "오늘 신고가 0건"과 똑같이 생겼다. 운영자는 처리할 게 없다고
 * 믿고 화면을 닫는데 실제로는 서버가 답을 못 준 것이다.
 *
 * <p>표는 이걸 쓰지 않는다. {@code DataTable} 이 로딩과 빈 상태를 이미 자기 안에서
 * 그린다. 규칙은 이렇다 - <b>표는 DataTable 이, 나머지 데이터 영역은 Loaded 가</b>
 * 상태를 맡는다. 화면이 직접 세 갈래 삼항을 쓰지 않는다.
 */
export function Loaded<T>({ query, skeleton, errorClassName, children }: LoadedProps<T>) {
  if (query.isPending) {
    return <>{skeleton ?? <Skeleton className="h-24" />}</>;
  }

  if (query.isError) {
    return (
      <ErrorState
        message={(query.error as Error).message}
        onRetry={() => query.refetch()}
        className={errorClassName}
      />
    );
  }

  return <>{children(query.data)}</>;
}
