import { Suspense } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  AlertTriangle,
  History,
  LayoutDashboard,
  LogOut,
  MessageSquareWarning,
  ScrollText,
  ServerCog,
  Search,
  ShieldBan,
  SpellCheck,
  UserCog,
  Users,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { useActionQueue } from '@/api/queries';
import type { ActionQueue } from '@/api/types';
import { cn } from '@/lib/cn';
import { useAuth } from '@/auth/AuthProvider';
import { EnvBadge } from '@/components/ui/EnvBadge';
import { Skeleton } from '@/components/ui/EmptyState';
import { formatNumber } from '@/lib/format';

/**
 * 대기 건수를 다는 자리.
 *
 * <p>배지를 다는 기준은 <b>일을 하면 0으로 줄어드는가</b>이다. 차단 IP 는 지금 몇 개가
 * 걸려 있는지를 말할 뿐 처리할 일이 아니라서 배지가 없다. 달아 두면 아무도 아무것도
 * 안 해도 숫자가 계속 떠 있고, 그런 배지는 곧 눈에서 지워진다.
 */
type QueueCount = (queue: ActionQueue) => number;

type NavItem = {
  to: string;
  label: string;
  icon: LucideIcon;
  count?: QueueCount;
  critical?: boolean;
};
type NavGroup = { heading: string; items: NavItem[] };

/**
 * 메뉴 옆 대기 건수.
 *
 * <p>0 이면 그리지 않는다. 회색 0 을 달아 두면 아홉 개 메뉴 옆에 0 이 줄줄이 서서,
 * 정작 숫자가 붙은 자리를 찾는 데 시간이 걸린다. <b>배지가 있다는 것 자체가 신호다.</b>
 *
 * <p>색은 격리 메시지에만 준다. 신고와 검열은 평소에도 쌓여 있는 것이 정상이라 늘
 * 코랄이면 그 색이 아무 뜻도 없어진다. 격리 메시지는 평소 0 이고 1 이 되는 순간이
 * 곧 사고라서, 회색이던 자리가 코랄로 <b>바뀌는</b> 것이 신호가 된다.
 */
function QueueBadge({ item, queue }: { item: NavItem; queue?: ActionQueue }) {
  if (!item.count || !queue) {
    return null;
  }

  const count = item.count(queue);
  if (count === 0) {
    return null;
  }

  return (
    <span
      className={cn(
        'ml-auto min-w-5 rounded-sm px-1 text-center text-2xs font-semibold leading-4',
        item.critical
          ? 'bg-attention-solid text-attention-on-solid'
          : 'bg-subtle text-ink-secondary',
      )}
    >
      {formatNumber(count)}
    </span>
  );
}

/**
 * 레일은 <b>무슨 일을 하는 자리인가</b>로 묶는다. 만든 순도 알파벳순도 아니다.
 *
 * <p>한때 운영과 조회와 관리 셋이었다. "운영"과 "관리"는 백오피스의 모든 화면에 해당하는
 * 말이라 어느 쪽에 넣어도 말이 됐고, 실제로 시스템과 조치 이력이 관리에 들어가 있었지만
 * 둘이 하는 일은 전혀 달랐다. <b>묶음 이름이 아무것도 걸러내지 못하면 묶지 않은 것과 같다.</b>
 *
 * <p>지금 셋은 각자 다른 질문에 답한다.
 *
 * <ul>
 *   <li><b>검토</b> - 사람이 판단해야 끝나는 일. 읽고 허용하거나 차단한다
 *   <li><b>기록</b> - 지나간 것을 찾아보는 일. 바꾸지 않는다
 *   <li><b>도구</b> - 서비스를 바꾸거나 다루는 일
 * </ul>
 *
 * <p>홈만 묶음 밖에 둔다. 어느 묶음에 넣어도 그 묶음의 뜻이 넓어지고, 맨 위 한 줄은
 * 이름표 없이도 무엇인지 분명하다.
 */
const HOME: NavItem = { to: '/', label: '홈', icon: LayoutDashboard };

const NAV: NavGroup[] = [
  {
    heading: '검토',
    items: [
      {
        to: '/reports',
        label: '신고',
        icon: MessageSquareWarning,
        count: (queue) => queue.pendingReports,
      },
      {
        to: '/profanity',
        label: '닉네임 검열',
        icon: SpellCheck,
        // 두 상태를 합쳐 센다. 레일에서 알아야 할 것은 "저기 손댈 게 남았나"지
        // AI 가 걸러낸 것과 판단 못한 것의 비율이 아니다. 그 구분은 화면이 한다.
        count: (queue) => queue.flaggedNicknames + queue.pendingNicknames,
      },
      { to: '/ip-blocks', label: 'IP 차단', icon: ShieldBan },
    ],
  },
  {
    heading: '기록',
    items: [
      { to: '/rooms', label: '방', icon: Search },
      { to: '/users', label: '유저', icon: Users },
      // 조치 이력이 여기 있는 것이 낯설어 보이지만, 하는 일은 방과 유저를 찾는 것과 같다.
      // 지나간 것을 찾아보고 아무것도 바꾸지 않는다.
      { to: '/audit-logs', label: '조치 이력', icon: History },
    ],
  },
  {
    heading: '도구',
    items: [
      { to: '/patch-notes', label: '패치노트', icon: ScrollText },
      { to: '/zzolbot', label: 'ZzolBot', icon: AlertTriangle },
      {
        to: '/system',
        label: '시스템',
        icon: ServerCog,
        count: (queue) => queue.deadLetters,
        critical: true,
      },
      { to: '/admins', label: '관리자', icon: UserCog },
    ],
  },
];

export function AdminLayout() {
  return (
    <div className="min-h-screen bg-canvas">
      <Rail />

      <div className="pl-rail-offset">
        {/* 상단 바를 없앴다.
         *
         * 거기 있던 것은 지금 메뉴 이름과 환경 배지 둘뿐이었다. 메뉴 이름은 레일에서 이미
         * 칠해져 있고 바로 아래 화면 제목이 같은 말을 크게 다시 적었다. 같은 단어가 한
         * 화면에 세 번 있었고, 그중 상단 바의 것만 작아서 <b>제목이 두 개인 것처럼</b>
         * 보였다. 환경 배지는 레일 머리로 옮겼다. 로고 옆이 오히려 눈에 먼저 걸린다.
         *
         * 56px 짜리 가로 띠가 사라지면서 첫 위젯이 화면 맨 위에서 시작한다. */}

        {/* 본문 폭은 <b>여기서만</b> 정한다.
         *
         * 화면마다 max-w 를 따로 적던 것을 걷어냈다. 홈은 1500, 폼은 1100, 목록은
         * 무제한이라 메뉴를 옮길 때마다 콘텐츠가 좌우로 튀었다. 카드 네 칸도 화면마다
         * 폭이 달라 같은 컴포넌트가 다르게 보였다.
         *
         * 1320px 은 레일을 뺀 본문 폭이다(레일 240px 은 이 div 의 padding 바깥에 있다).
         * 카드 네 칸을 늘어놓으면 한 칸이 약 315px 로, 라벨과 두세 자리 숫자가 한 줄에
         * 들어가면서 안이 비어 보이지 않는 크기다. 1600px 에서는 한 칸이 385px 이라
         * 같은 내용에 여백만 늘어 카드가 헐거워 보였다.
         *
         * 열이 많은 표는 카드 안에서 가로 스크롤한다. 그 한 화면 때문에 나머지
         * 전부를 넓히지 않는다.
         *
         * 상하 여백을 좌우보다 조금 크게 둔다. 상단 바 바로 아래에 제목이 붙으면
         * 두 줄이 한 덩어리로 읽혀 위계가 무너진다. */}
        <main className="mx-auto w-full max-w-[1320px] px-6 pb-10 pt-6">
          {/* 화면 코드를 라우트 단위로 나눠 받으므로 경계가 필요하다. 레일과 상단 바
            * 바깥이 아니라 여기 두는 이유는, 화면을 옮길 때 네비게이션까지 사라졌다
            * 다시 나타나면 어디에 있는지를 매번 다시 찾게 되기 때문이다. */}
          <Suspense fallback={<Skeleton className="h-64" />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  );
}

function Rail() {
  // 레일에서 한 번만 부른다. 화면마다 따로 부르면 같은 숫자를 여러 번 물어보게 되고,
  // 화면을 옮길 때마다 배지가 깜빡인다. 여기 있으면 화면이 바뀌어도 레일은 안 다시 그린다.
  const queue = useActionQueue();

  return (
    <nav
      aria-label="주 메뉴"
      // 화면에 붙은 세로 슬래브가 아니라 캔버스 위에 떠 있는 패널이다.
      // 가장자리에서 12px 떼어 두면 레일도 카드와 같은 종류의 물체로 읽혀서,
      // 화면 전체가 한 장의 문서가 아니라 위젯이 놓인 판처럼 보인다.
      className="fixed inset-y-rail-inset left-rail-inset z-20 flex w-rail flex-col overflow-hidden rounded-lg border border-rail-line bg-rail shadow-card"
    >
      {/* 워드마크만 둔다. 캐릭터까지 붙이면 좁은 레일 머리에 그래픽이 두 개가 되어 번잡하다.
       * 캐릭터는 파비콘으로만 쓴다. 탭에서는 정사각형 마크가 필요하기 때문이다.
       *
       * 구분선을 뺐다. 레일이 떠 있는 패널이 되면서 "화면을 한 줄로 가로지르는 선"이
       * 애초에 성립하지 않는다. 패널 안에 선을 그으면 그 패널만 두 칸으로 쪼개 보인다. */}
      <div className="flex h-topbar shrink-0 items-center gap-2.5 px-4">
        <img src="/brand/logo.svg" alt="ZZOL" className="h-[18px]" />
        <span className="text-2xs font-medium tracking-wide text-ink-muted">백오피스</span>
        <EnvBadge className="ml-auto" />
      </div>

      <div className="flex-1 overflow-y-auto px-3 pb-4">
        {/* 홈은 이름표 없이 맨 위 한 줄이다. 아래 묶음들과 간격으로 갈린다. */}
        <ul className="flex flex-col gap-0.5">
          <NavRow item={HOME} queue={queue.data} />
        </ul>

        {NAV.map((group) => (
          <div key={group.heading} className="mt-5 first:mt-0">
            <p className="px-2 pb-2 text-2xs font-semibold tracking-wider text-rail-ink-heading">
              {group.heading}
            </p>
            <ul className="flex flex-col gap-0.5">
              {group.items.map((item) => (
                <NavRow key={item.to} item={item} queue={queue.data} />
              ))}
            </ul>
          </div>
        ))}
      </div>

      <AccountFooter />
    </nav>
  );
}

/** 메뉴 한 줄. 홈은 묶음 밖에서, 나머지는 묶음 안에서 같은 모양으로 쓴다. */
function NavRow({ item, queue }: { item: NavItem; queue?: ActionQueue }) {
  return (
    <li>
      <NavLink
        to={item.to}
        end={item.to === '/'}
        className={({ isActive }) =>
          cn(
            // 액티브는 둥근 틴트 한 겹으로 끝낸다. 왼쪽 세로 막대까지 겹치면
            // 표시가 두 개가 되어 오히려 지저분해진다.
            'flex h-8 items-center gap-2.5 rounded-md px-2.5 text-sm transition-colors',
            isActive
              ? 'bg-rail-active font-semibold text-rail-ink-active'
              : 'text-rail-ink hover:bg-rail-hover hover:text-ink',
          )
        }
      >
        {({ isActive }) => (
          <>
            <item.icon
              className={cn('size-4 shrink-0', isActive ? 'text-accent' : 'text-ink-muted')}
              aria-hidden
            />
            {item.label}
            <QueueBadge item={item} queue={queue} />
          </>
        )}
      </NavLink>
    </li>
  );
}

/**
 * 레일 바닥의 계정 영역. 지금 누구로 들어와 있는지가 늘 보여야 한다.
 * 관리자 조치는 전부 감사 로그에 이 이메일로 남으므로, 남의 계정으로 눌렀다는 것을
 * 나중에 알게 되는 상황을 만들면 안 된다.
 */
function AccountFooter() {
  const auth = useAuth();
  if (auth.status !== 'authenticated') {
    return null;
  }

  return (
    <div className="shrink-0 border-t border-rail-line p-3">
      <div className="flex items-center gap-2">
        <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-subtle text-2xs font-semibold text-ink-secondary">
          {auth.email.slice(0, 1).toUpperCase()}
        </span>
        <span className="min-w-0 flex-1 truncate text-xs text-ink-secondary" title={auth.email}>
          {auth.email}
        </span>
        <button
          type="button"
          onClick={auth.logout}
          aria-label="로그아웃"
          className="flex size-7 shrink-0 items-center justify-center rounded-md text-ink-muted transition-colors hover:bg-rail-hover hover:text-ink"
        >
          <LogOut className="size-3.5" aria-hidden />
        </button>
      </div>
    </div>
  );
}
