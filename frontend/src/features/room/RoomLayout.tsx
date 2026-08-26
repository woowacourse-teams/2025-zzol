import { useNavigationGuard } from '@/hooks/useNavigateGuard';
import { useRoomAccessGuard } from '@/hooks/useRoomAccessGuard';
import { Outlet } from 'react-router-dom';
import { useRestoreParticipants } from './hooks/useRestoreParticipants';

const RoomLayout = () => {
  useNavigationGuard();
  useRoomAccessGuard();
  // 로비·게임·룰렛 전부 이 레이아웃 아래라 리프레시 복구를 한 곳에서 처리한다(#1688)
  useRestoreParticipants();
  return <Outlet />;
};

export default RoomLayout;
