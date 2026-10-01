import { GOOGLE_CLIENT_ID } from '@/lib/env';

/**
 * 구글 Identity Services 로 ID 토큰을 받는다.
 *
 * <p>리다이렉트가 아니라 팝업이다. 리다이렉트 플로우는 서버에 콜백 엔드포인트가 있어야
 * 하는데, 그러면 기존 유저 로그인 경로(OAuthSuccessHandler)를 건드리게 된다.
 * 팝업으로 ID 토큰만 받아 교환하면 그 경로를 전혀 손대지 않는다.
 *
 * <p>받는 것은 `credential`, 곧 ID 토큰이다. access token 이 아니다.
 * 구글 API 를 호출할 일이 없으므로 access token 은 필요 없다.
 */

type CredentialResponse = { credential?: string };

type GoogleAccounts = {
  accounts: {
    id: {
      initialize(config: {
        client_id: string;
        callback: (response: CredentialResponse) => void;
        auto_select?: boolean;
        cancel_on_tap_outside?: boolean;
      }): void;
      renderButton(
        parent: HTMLElement,
        options: {
          type?: 'standard' | 'icon';
          theme?: 'outline' | 'filled_blue' | 'filled_black';
          size?: 'small' | 'medium' | 'large';
          text?: 'signin_with' | 'signup_with' | 'continue_with';
          shape?: 'rectangular' | 'pill';
          width?: number;
          locale?: string;
        },
      ): void;
      disableAutoSelect(): void;
    };
  };
};

declare global {
  interface Window {
    google?: GoogleAccounts;
  }
}

const SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

let scriptPromise: Promise<void> | null = null;

/** 스크립트를 한 번만 넣는다. 로그인 화면을 여러 번 오가도 태그가 쌓이지 않는다. */
export function loadGoogleScript(): Promise<void> {
  if (scriptPromise) {
    return scriptPromise;
  }
  scriptPromise = new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src = SCRIPT_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      // 실패하면 다음 시도에서 다시 넣을 수 있게 캐시를 비운다.
      scriptPromise = null;
      reject(new Error('구글 로그인 스크립트를 불러오지 못했습니다.'));
    };
    document.head.appendChild(script);
  });
  return scriptPromise;
}

export function isGoogleConfigured(): boolean {
  return GOOGLE_CLIENT_ID.length > 0;
}

/**
 * 구글 버튼을 그리고, 로그인이 끝나면 ID 토큰을 콜백으로 넘긴다.
 *
 * <p>버튼을 직접 만들지 않고 구글이 그리게 한다. 구글 브랜드 가이드가 버튼 모양을
 * 규정하고 있고, 직접 만든 버튼으로 `prompt()` 를 부르는 방식은 팝업 차단에 걸리기 쉽다.
 */
export async function renderGoogleButton(
  parent: HTMLElement,
  onCredential: (idToken: string) => void,
): Promise<void> {
  await loadGoogleScript();
  const id = window.google?.accounts?.id;
  if (!id) {
    throw new Error('구글 로그인을 초기화하지 못했습니다.');
  }

  id.initialize({
    client_id: GOOGLE_CLIENT_ID,
    callback: (response) => {
      if (response.credential) {
        onCredential(response.credential);
      }
    },
    // 자동 선택을 끈다. 백오피스는 "지금 이 계정으로 들어간다"를 사람이 확인해야 한다.
    auto_select: false,
    cancel_on_tap_outside: true,
  });

  id.renderButton(parent, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    text: 'signin_with',
    shape: 'rectangular',
    locale: 'ko',
    // 폭을 재서 넘긴다. GSI 는 CSS 로 늘릴 수 없고 px 숫자만 받는다. 안 넘기면 버튼이
    // 내용 폭만큼만 그려져 카드 안에서 혼자 좁아 보인다. 구글이 받는 상한은 400 이다.
    width: Math.min(parent.clientWidth || 320, 400),
  });
}

export function disableGoogleAutoSelect(): void {
  window.google?.accounts?.id?.disableAutoSelect();
}
