import { QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { lazy } from 'react';
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { LegacyRedirect } from '@/routes/LegacyRedirect';
import { ApiError } from '@/api/client';
import { AuthProvider, useAuth } from '@/auth/AuthProvider';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Skeleton } from '@/components/ui/EmptyState';
import { HomePage } from '@/pages/HomePage';
import { LoginPage } from '@/pages/LoginPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

/**
 * 화면을 라우트 단위로 나눠 받는다.
 *
 * <p>전부 정적으로 가져오면 로그인 화면 하나 띄우는 데 백오피스 전체 코드를 내려받는다.
 * 운영자는 대개 홈과 처리할 큐 한둘만 보고 닫으므로, 안 여는 화면의 무게까지 첫 진입에
 * 지불할 이유가 없다.
 *
 * <p>로그인, 홈, 없는 주소는 정적으로 둔다. 셋은 첫 렌더에 필요하거나 곧바로 이어지는
 * 화면이라, 쪼개면 흰 화면이 한 번 더 깜빡이는 값만 치른다.
 */
const AdminAccountsPage = lazy(() =>
  import('@/pages/AdminAccountsPage').then((m) => ({ default: m.AdminAccountsPage })),
);
const AuditLogsPage = lazy(() =>
  import('@/pages/AuditLogsPage').then((m) => ({ default: m.AuditLogsPage })),
);
const IpBlocksPage = lazy(() =>
  import('@/pages/IpBlocksPage').then((m) => ({ default: m.IpBlocksPage })),
);
const ProfanityPage = lazy(() =>
  import('@/pages/ProfanityPage').then((m) => ({ default: m.ProfanityPage })),
);
const PatchNoteFormPage = lazy(() =>
  import('@/pages/PatchNoteFormPage').then((m) => ({ default: m.PatchNoteFormPage })),
);
const PatchNotesPage = lazy(() =>
  import('@/pages/PatchNotesPage').then((m) => ({ default: m.PatchNotesPage })),
);
const ReportsPage = lazy(() =>
  import('@/pages/ReportsPage').then((m) => ({ default: m.ReportsPage })),
);
const RoomsPage = lazy(() => import('@/pages/RoomsPage').then((m) => ({ default: m.RoomsPage })));
const UsersPage = lazy(() => import('@/pages/UsersPage').then((m) => ({ default: m.UsersPage })));
const SystemPage = lazy(() => import('@/pages/SystemPage').then((m) => ({ default: m.SystemPage })));
const ZzolBotPage = lazy(() =>
  import('@/pages/ZzolBotPage').then((m) => ({ default: m.ZzolBotPage })),
);

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      // 401 은 화면마다 처리하지 않는다. 토큰은 client 가 이미 지웠고,
      // 다음 렌더에서 RequireAuth 가 로그인으로 보낸다.
      if (error instanceof ApiError && error.isUnauthorized) {
        queryClient.clear();
      }
    },
  }),
  defaultOptions: {
    queries: {
      // 운영 화면은 오래된 숫자를 보여주면 안 된다. 조치하고 돌아왔을 때 이전 값이
      // 남아 있으면 조치가 안 먹은 것으로 오해한다.
      staleTime: 10_000,
      refetchOnWindowFocus: true,
      // 인증/권한 오류는 재시도해도 결과가 같다. 한 번 더 물어봐야 사용자만 기다린다.
      retry: (failureCount, error) =>
        error instanceof ApiError && (error.isUnauthorized || error.isForbidden)
          ? false
          : failureCount < 1,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<RequireAuth />}>
              <Route element={<AdminLayout />}>
                <Route index element={<HomePage />} />
                <Route path="/reports" element={<ReportsPage />} />
                <Route path="/profanity" element={<ProfanityPage />} />
                <Route path="/ip-blocks" element={<IpBlocksPage />} />
                <Route path="/rooms" element={<RoomsPage />} />
                <Route path="/users" element={<UsersPage />} />
                {/* 상세가 라우트에서 패널로 옮겨 갔다. 옛 주소는 목록으로 보낸다 - 패널을
                 * 여는 데 필요한 것은 id 인데 그 id 가 검색 결과 안에 있다는 보장이 없고,
                 * 억지로 열면 목록에 없는 항목의 패널만 덩그러니 뜬다. */}
                <Route path="/rooms/:roomId" element={<LegacyRedirect to="/rooms" />} />
                <Route path="/users/:userId" element={<LegacyRedirect to="/users" />} />
                {/* 방과 유저를 한 화면에 합쳤다가 되돌렸다. 표 두 개가 세로로 쌓이면서
                 * 어느 표를 보고 있는지가 흐려졌다.
                 *
                 * 검색어를 들고 넘어간다. 이 주소로 오는 링크는 "이 코드를 찾아라"라는
                 * 뜻을 쿼리에 담고 오는데, 경로만 넘기면 목록만 열리고 코드는 사라진다. */}
                <Route path="/trace" element={<LegacyRedirect to="/rooms" />} />
                {/* 서비스 분석을 홈에 흡수했다. 옛 주소는 홈으로 보낸다. */}
                <Route path="/games" element={<LegacyRedirect to="/" />} />
                <Route path="/patch-notes" element={<PatchNotesPage />} />
                <Route path="/patch-notes/new" element={<PatchNoteFormPage />} />
                <Route path="/patch-notes/:id" element={<PatchNoteFormPage />} />
                <Route path="/admins" element={<AdminAccountsPage />} />
                <Route path="/zzolbot" element={<ZzolBotPage />} />
                <Route path="/system" element={<SystemPage />} />
                {/* 화면 이름을 "시스템"으로 맞추면서 주소도 옮겼다. 옛 주소는 살려 둔다. */}
                <Route path="/ops" element={<LegacyRedirect to="/system" />} />
                <Route path="/audit-logs" element={<AuditLogsPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Route>
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

/**
 * 로그인 확인이 끝나기 전에는 아무것도 그리지 않는다.
 * 확인 중에 화면을 그리면 로그인 상태인데도 로그인 화면이 한 번 스쳐 지나간다.
 */
function RequireAuth() {
  const auth = useAuth();

  if (auth.status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas">
        <Skeleton className="h-8 w-32" />
      </div>
    );
  }

  if (auth.status === 'anonymous') {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
