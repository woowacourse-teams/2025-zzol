import { ComponentType, lazy } from 'react';
import { createBrowserRouter, Outlet } from 'react-router-dom';
import App from './App';
import MiniGameProviders from './features/miniGame/context/MiniGameProviders';
import RoomLayout from './features/room/RoomLayout';
import { EntryNamePage, HomePage } from './pages';

const RELOAD_KEY = 'zzol-chunk-reload-at';

// 재배포로 옛 해시 청크를 못 받으면 새 index.html 을 받도록 한 번 새로고침한다(#1857).
// 10초 안에 또 실패하면 새로고침을 반복하지 않고 에러를 그대로 올린다.
const canReload = () => {
  try {
    const last = Number(sessionStorage.getItem(RELOAD_KEY));
    if (Date.now() - last < 10_000) return false;
    sessionStorage.setItem(RELOAD_KEY, String(Date.now()));
    return true;
  } catch {
    return false;
  }
};

const lazyWithReload = <T extends ComponentType>(factory: () => Promise<{ default: T }>) =>
  lazy(() =>
    factory().catch((error: Error) => {
      if (error.name !== 'ChunkLoadError' || !canReload()) throw error;
      window.location.reload();
      return new Promise<never>(() => {});
    })
  );

const LobbyPage = lazyWithReload(
  /*webpackChunkName: "lobbyPage"*/ () => import('./features/room/lobby/pages/LobbyPage')
);
const MiniGamePlayPage = lazyWithReload(
  () =>
    import(
      /*webpackChunkName: "miniGamePlayPage"*/ './features/miniGame/pages/MiniGamePlayPage/MiniGamePlayPage'
    )
);
const MiniGameReadyPage = lazyWithReload(
  () =>
    import(
      /*webpackChunkName: "miniGameReadyPage"*/ './features/miniGame/pages/MiniGameReadyPage/MiniGameReadyPage'
    )
);
const MiniGameResultPage = lazyWithReload(
  () =>
    import(
      /*webpackChunkName: "miniGameResultPage"*/ './features/miniGame/pages/MiniGameResultPage/MiniGameResultPage'
    )
);
const NotFoundPage = lazyWithReload(
  /*webpackChunkName: "notFoundPage"*/ () => import('./features/notFound/pages/NotFoundPage')
);
const RoulettePlayPage = lazyWithReload(
  () =>
    import(
      /*webpackChunkName: "roulettePlayPage"*/ './features/room/roulette/pages/RoulettePlayPage/RoulettePlayPage'
    )
);
const RouletteResultPage = lazyWithReload(
  () =>
    import(
      /*webpackChunkName: "rouletteResultPage"*/ './features/room/roulette/pages/RouletteResultPage/RouletteResultPage'
    )
);
const QRJoinPage = lazyWithReload(
  () => import(/*webpackChunkName: "qrJoinPage"*/ './features/join/pages/QRJoinPage')
);
const OAuthCallbackPage = lazyWithReload(
  () => import(/*webpackChunkName: "oauthCallbackPage"*/ './features/auth/pages/OAuthCallbackPage')
);
const TermsAgreementPage = lazyWithReload(
  () =>
    import(/*webpackChunkName: "termsAgreementPage"*/ './features/auth/pages/TermsAgreementPage')
);
const PrivacyPage = lazyWithReload(
  () => import(/*webpackChunkName: "privacyPage"*/ './features/privacy/pages/PrivacyPage')
);
const SeoContentPage = lazyWithReload(
  () => import(/*webpackChunkName: "seoContentPage"*/ './features/seo/pages/SeoContentPage')
);

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'entry',
        children: [{ path: 'name', element: <EntryNamePage /> }],
      },
      {
        path: 'room/:joinCode',
        element: <RoomLayout />,
        children: [
          { path: 'lobby', element: <LobbyPage /> },
          { path: 'roulette/play', element: <RoulettePlayPage /> },
          { path: 'roulette/result', element: <RouletteResultPage /> },
          {
            path: ':miniGameType',
            element: (
              <MiniGameProviders>
                <Outlet />
              </MiniGameProviders>
            ),
            children: [
              { path: 'ready', element: <MiniGameReadyPage /> },
              { path: 'play', element: <MiniGamePlayPage /> },
              { path: 'result', element: <MiniGameResultPage /> },
            ],
          },
        ],
      },
      {
        path: 'join/:joinCode',
        element: <QRJoinPage />,
      },
      {
        path: 'auth/callback',
        element: <OAuthCallbackPage />,
      },
      {
        path: 'auth/terms',
        element: <TermsAgreementPage />,
      },
      {
        path: 'privacy',
        element: <PrivacyPage />,
      },
      {
        path: 'guide',
        element: <SeoContentPage />,
      },
      {
        path: 'games',
        children: [
          { index: true, element: <SeoContentPage /> },
          { path: ':slug', element: <SeoContentPage /> },
        ],
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);

export default router;
