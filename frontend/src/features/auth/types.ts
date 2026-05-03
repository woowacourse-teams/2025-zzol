export type OAuthProvider = 'google' | 'kakao' | 'naver';

export type User = {
  userCode: string;
  nickname: string;
  provider: OAuthProvider;
};

export type Tokens = {
  accessToken: string;
};

export type OAuthCallbackResponse =
  | { isNewUser: false; accessToken: string; refreshToken: string }
  | { isNewUser: true; tempToken: string; accessToken: null; refreshToken: null };
