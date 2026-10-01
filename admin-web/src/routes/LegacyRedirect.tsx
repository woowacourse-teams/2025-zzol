import { Navigate, useLocation } from 'react-router-dom';

/**
 * 옛 주소를 새 주소로 넘긴다. <b>검색어는 들고 간다.</b>
 *
 * <p>{@code <Navigate to="/rooms" />} 로만 두면 경로만 남고 쿼리가 버려진다. 옛 주소로
 * 오는 링크는 대개 무언가를 찾으라는 뜻을 함께 들고 온다({@code /trace?q=ABCD}). 쿼리를
 * 버리면 목적지에 닿기는 하는데 <b>찾으라던 것이 사라져</b> 사용자가 코드를 다시 친다.
 *
 * <p>해시도 함께 넘긴다. 지금 쓰는 곳은 없지만 이 컴포넌트가 하는 일이 "주소를 옮기되
 * 내용은 그대로 두는 것"이라, 한 조각만 빼 두면 다음에 쓸 때 같은 일이 반복된다.
 */
export function LegacyRedirect({ to }: { to: string }) {
  const location = useLocation();
  return <Navigate to={{ pathname: to, search: location.search, hash: location.hash }} replace />;
}
