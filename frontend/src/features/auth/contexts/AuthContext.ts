import { createContext, useContext } from 'react';
import { OAuthProvider, User } from '../types';

type AuthContextType = {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (provider: OAuthProvider) => void;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  updateNickname: (nickname: string) => Promise<void>;
};

export const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth 는 AuthProvider 안에서 사용해야 합니다.');
  }
  return context;
};
