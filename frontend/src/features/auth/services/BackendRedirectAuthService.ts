import { authApi } from '../api/authApi';
import { TokenStore } from '../tokens/TokenStore';
import { OAuthProvider } from '../types';
import { AuthService } from './AuthService';

// Spring Security 표준 OAuth2 Client 방식
// 프론트 → /oauth2/authorization/{provider} redirect
//        → provider 인증 → 백엔드 /login/oauth2/code/{provider} 콜백
//        → OAuthSuccessHandler → 프론트 /callback?code=xxx 으로 redirect
//        → 프론트가 POST /auth/token { code } 호출 → accessToken 수신
//           (refreshToken은 HttpOnly 쿠키로 자동 설정됨)
export class BackendRedirectAuthService implements AuthService {
  constructor(private readonly tokenStore: TokenStore) {}

  startOAuth(provider: OAuthProvider): void {
    window.location.href = `${process.env.API_URL}/oauth2/authorization/${provider}`;
  }

  async handleCallback(params: URLSearchParams): Promise<void> {
    const code = params.get('code');

    if (!code) {
      throw new Error('OAuth callback에 code가 없습니다');
    }

    const tokens = await authApi.token(code);
    this.tokenStore.setTokens(tokens);
  }

  async refresh(): Promise<void> {
    const tokens = await authApi.refresh();
    this.tokenStore.setTokens(tokens);
  }

  async logout(): Promise<void> {
    try {
      await authApi.logout();
    } finally {
      this.tokenStore.clearTokens();
    }
  }
}
