import { TokenStore } from '../tokens/TokenStore';
import { User, OAuthProvider } from '../types';
import { AuthService } from './AuthService';

type MockAuthServiceOptions = {
  onLogin: (user: User) => void;
  onLogout: () => void;
};

const MOCK_USERS: Record<OAuthProvider, User> = {
  google: { userCode: 'MOCK1', nickname: '구글 유저', provider: 'google' },
  kakao: { userCode: 'MOCK2', nickname: '카카오 유저', provider: 'kakao' },
  naver: { userCode: 'MOCK3', nickname: '네이버 유저', provider: 'naver' },
};

// 개발/Storybook용 mock — 실제 OAuth 없이 즉시 로그인 처리
export class MockAuthService implements AuthService {
  private lastProvider: OAuthProvider | null = null;

  constructor(
    private readonly tokenStore: TokenStore,
    private readonly callbacks: MockAuthServiceOptions
  ) {}

  startOAuth(provider: OAuthProvider): void {
    this.lastProvider = provider;
    this.tokenStore.setTokens({ accessToken: `mock-access-token-${provider}` });
    this.callbacks.onLogin(MOCK_USERS[provider]);
  }

  async handleCallback(params: URLSearchParams): Promise<void> {
    void params;
    // mock 환경에서는 startOAuth에서 이미 처리됨
  }

  async refresh(): Promise<void> {
    if (!this.lastProvider) return;
    this.tokenStore.setTokens({ accessToken: `mock-access-token-refreshed-${this.lastProvider}` });
  }

  async logout(): Promise<void> {
    this.tokenStore.clearTokens();
    this.lastProvider = null;
    this.callbacks.onLogout();
  }
}
