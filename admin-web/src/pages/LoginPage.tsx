import { useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { toLoginErrorMessage, useAuth } from '@/auth/AuthProvider';
import { isGoogleConfigured, renderGoogleButton } from '@/auth/google';
import { EnvBadge } from '@/components/ui/EnvBadge';

/**
 * 로그인. GAMSS 백오피스 로그인 화면의 구성을 그대로 따랐다.
 *
 * <p>가운데 정렬한 단일 카드, 그 위에 로고 블록, 카드 안에는 버튼 하나와 안내 한 줄,
 * 아래에 서비스 이름 한 줄. 배경에는 상단 중앙 하이라이트와 44px 격자를 아주 옅게 깐다.
 * 로그인 화면은 하루에 한 번 지나가는 곳이라 고를 것도 읽을 것도 없어야 한다.
 *
 * <p><b>두 가지만 다르게 했다.</b>
 *
 * <p>하나, 버튼을 직접 만들지 않고 구글이 그리게 둔다. GAMSS 는 Firebase 의
 * {@code signInWithPopup} 을 써서 아무 버튼에나 붙일 수 있지만, 우리는 GSI 로 ID 토큰을
 * 직접 받는다. 그쪽은 커스텀 버튼에서 팝업을 띄울 안정적인 방법이 없다
 * ({@code prompt()} 는 One Tap 이라 브라우저가 억제할 수 있다). 대신 outline 테마와
 * 카드 폭을 넘겨 모양을 맞췄다.
 *
 * <p>둘, 실패 사유를 화면에 띄운다. GAMSS 는 authProvider 가 만든 한국어 메시지를
 * 어디에도 렌더하지 않아, 허용목록에 없는 계정으로 누르면 버튼만 잠깐 깜빡이고 끝난다.
 * 왜 안 되는지 모르는 채로 다시 누르게 된다.
 */
export function LoginPage() {
  const auth = useAuth();
  const googleButtonRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isGoogleConfigured() || !googleButtonRef.current) {
      return;
    }
    renderGoogleButton(googleButtonRef.current, (idToken) => {
      setError(null);
      auth.loginWithGoogle(idToken).catch((cause) => setError(toLoginErrorMessage(cause)));
    }).catch((cause) => setError(toLoginErrorMessage(cause)));
  }, [auth]);

  if (auth.status === 'authenticated') {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-canvas px-4">
      {/* 배경 두 겹. 둘 다 장식이라 스크린리더와 포인터에서 뺀다.
        * 흰 카드 하나만 덩그러니 놓으면 화면이 비어 보이는데, 여기를 채우려고 그림이나
        * 문구를 넣으면 로그인 말고 읽을 것이 생긴다. 격자는 읽히지 않으면서 바닥만 만든다. */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 [background-image:radial-gradient(circle_at_50%_0%,rgb(16_24_40/0.04),transparent_55%)]"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 opacity-[0.35] [background-image:linear-gradient(to_right,var(--border)_1px,transparent_1px),linear-gradient(to_bottom,var(--border)_1px,transparent_1px)] [background-size:44px_44px] [mask-image:radial-gradient(ellipse_at_center,black,transparent_70%)]"
      />

      <div className="relative w-full max-w-sm">
        {/* 로고 자리에 글자 타일 대신 워드마크를 쓴다. GAMSS 는 로고 자산이 없어
          * 머리글자 한 자를 타일에 넣었는데, 우리는 실제 로고가 있다. */}
        <div className="mb-7 flex flex-col items-center gap-3">
          <h1>
            <img src="/brand/logo.svg" alt="ZZOL" className="h-7" />
          </h1>
          <div className="flex flex-col items-center gap-2">
            <p className="text-sm text-ink-muted">백오피스 관리자 콘솔</p>
            {/* 어느 환경에 들어가는지는 로그인 <b>전에</b> 알아야 한다.
              * prod 백오피스에서 조치를 누른 뒤에 알아차리면 늦다. */}
            <EnvBadge />
          </div>
        </div>

        <div className="rounded-lg border border-border-default bg-surface p-6 shadow-popover">
          {isGoogleConfigured() ? (
            <div ref={googleButtonRef} className="flex justify-center [&>div]:!w-full" />
          ) : (
            <p className="rounded-md border border-border-default bg-subtle px-3 py-2 text-xs leading-relaxed text-ink-secondary">
              구글 클라이언트 ID가 설정되지 않았습니다.
              <br />
              <code className="font-mono">VITE_GOOGLE_CLIENT_ID</code> 를 채워야 로그인할 수 있습니다.
            </p>
          )}

          <p className="mt-4 text-center text-xs text-ink-muted">
            접근이 허용된 이메일만 로그인할 수 있습니다.
          </p>

          {error && (
            <p className="mt-4 flex items-start gap-1.5 rounded-md bg-attention-bg px-3 py-2 text-xs leading-relaxed text-attention">
              <span className="mt-1 size-1.5 shrink-0 rounded-full bg-attention-mark" aria-hidden />
              {error}
            </p>
          )}
        </div>

        <p className="mt-6 text-center text-xs text-ink-muted/70">ZZOL · 미니게임 당첨자 추첨</p>
      </div>
    </div>
  );
}
