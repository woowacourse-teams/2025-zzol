import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';

/**
 * 없는 주소. 화면이 다 갖춰진 뒤로는 "아직 안 만들었다"가 아니라 "없다"가 맞다.
 * 오타로 들어온 경우가 대부분이라 홈으로 돌려보낸다.
 */
export function NotFoundPage() {
  return (
    <EmptyState
      title="없는 주소입니다"
      description="주소가 바뀌었거나 잘못 입력했을 수 있습니다. 왼쪽 메뉴에서 찾아보세요."
      action={
        <Button asChild variant="primary">
          <Link to="/">홈으로</Link>
        </Button>
      }
    />
  );
}
