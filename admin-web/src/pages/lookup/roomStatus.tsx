import type { RoomState } from '@/api/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { roomStateLabel } from '@/lib/labels';

/**
 * 방이 어디까지 갔는지. 완주한 방은 물러나고 중간에 멈춘 방이 눈에 걸려야 한다.
 *
 * <p>표와 패널이 함께 쓰므로 화면 파일이 아니라 여기 둔다. 예전에는 방 목록 화면이
 * 내보내고 방 상세 화면이 가져다 썼는데, 화면이 화면을 import 하면 지울 때 딸려 온다.
 */
export function roomStatusBadge(status: RoomState) {
  if (status === 'DONE') {
    return <StatusBadge tone="muted">완주</StatusBadge>;
  }
  if (status === 'READY') {
    return <StatusBadge tone="attention">시작 안 함</StatusBadge>;
  }
  // 진행 중인 상태들이다. 서버 enum 이 그대로 찍히던 자리라 한글로 바꾼다.
  return <StatusBadge tone="neutral">{roomStateLabel(status)}</StatusBadge>;
}
