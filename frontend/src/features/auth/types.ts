export type OAuthProvider = 'google' | 'kakao' | 'naver';

export type User = {
  userCode: string;
  nickname: string;
  provider: OAuthProvider;
};

export type Tokens = {
  accessToken: string;
  refreshToken: string;
};
