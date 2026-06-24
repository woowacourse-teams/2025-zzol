import { useNavigationGuard } from '@/hooks/useNavigateGuard';
import { useRoomAccessGuard } from '@/hooks/useRoomAccessGuard';
import { Outlet } from 'react-router-dom';

const RoomLayout = () => {
  useNavigationGuard();
  useRoomAccessGuard();
  return <Outlet />;
};

export default RoomLayout;
