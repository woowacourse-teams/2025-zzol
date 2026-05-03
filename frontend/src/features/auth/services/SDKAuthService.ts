import { TokenStore } from '../tokens/TokenStore';
import { OAuthProvider } from '../types';
import { AuthService } from './AuthService';

// 프론트 SDK로 인가코드를 직접 받아 백엔드 /auth/{provider}/login에 전달하는 방식
// 백엔드 결정 후 각 provider SDK 설치 + 실제 구현
export class SDKAuthService implements AuthService {
  constructor(private readonly tokenStore: TokenStore) {
    void tokenStore;
  }

  startOAuth(provider: OAuthProvider): void {
    void provider;
    throw new Error(
      'SDKAuthService: 아직 구현되지 않았습니다. BackendRedirectAuthService를 사용하세요.'
    );
  }

  async handleCallback(params: URLSearchParams): Promise<void> {
    void params;
    throw new Error('SDKAuthService: 아직 구현되지 않았습니다.');
  }

  async refresh(): Promise<void> {
    throw new Error('SDKAuthService: 아직 구현되지 않았습니다.');
  }

  async logout(): Promise<void> {
    throw new Error('SDKAuthService: 아직 구현되지 않았습니다.');
  }
}
