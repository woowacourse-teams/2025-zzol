import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '@/components/@common/Button/Button';
import Layout from '@/layouts/Layout';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import useToast from '@/components/@common/Toast/useToast';
import { useAuth } from '../contexts/AuthContext';
import { tokenStore } from '../tokens';
import { authApi } from '../api/authApi';
import * as S from './TermsAgreementPage.styled';

const TermsAgreementPage = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { refreshUser } = useAuth();
  const [termsChecked, setTermsChecked] = useState(false);
  const [privacyChecked, setPrivacyChecked] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const allChecked = termsChecked && privacyChecked;

  const handleAgree = async () => {
    const tempToken = storageManager.getItem(STORAGE_KEYS.TEMP_TOKEN, 'sessionStorage');
    if (!tempToken) {
      showToast({ message: '인증 정보가 만료되었습니다. 다시 로그인해 주세요.', type: 'error' });
      navigate('/', { replace: true });
      return;
    }

    setIsLoading(true);
    try {
      const tokens = await authApi.agreeTerms(tempToken);
      storageManager.removeItem(STORAGE_KEYS.TEMP_TOKEN, 'sessionStorage');
      tokenStore.setTokens(tokens);
      await refreshUser();
      navigate('/', { replace: true });
    } catch {
      showToast({ message: '약관 동의 처리 중 오류가 발생했습니다.', type: 'error' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Layout>
      <Layout.Content>
        <S.Container>
          <S.Header>
            <S.Title>서비스 이용을 위해{'\n'}약관에 동의해 주세요</S.Title>
            <S.Subtitle>필수 항목에 모두 동의해야 서비스를 이용할 수 있어요.</S.Subtitle>
          </S.Header>

          <S.TermsList>
            <S.TermsItem>
              <S.Checkbox
                type="checkbox"
                id="terms"
                checked={termsChecked}
                onChange={(e) => setTermsChecked(e.target.checked)}
              />
              <S.TermsLabel htmlFor="terms">
                <S.TermsTitle>[필수] 서비스 이용약관 동의</S.TermsTitle>
              </S.TermsLabel>
            </S.TermsItem>

            <S.TermsItem>
              <S.Checkbox
                type="checkbox"
                id="privacy"
                checked={privacyChecked}
                onChange={(e) => setPrivacyChecked(e.target.checked)}
              />
              <S.TermsLabel htmlFor="privacy">
                <S.TermsTitle>[필수] 개인정보 수집·이용 동의</S.TermsTitle>
                <S.TermsDesc>
                  수집항목: 이메일·닉네임 / 목적: 회원 식별 / 보유기간: 탈퇴 시까지
                </S.TermsDesc>
              </S.TermsLabel>
            </S.TermsItem>
          </S.TermsList>

          <S.ButtonWrapper>
            <Button
              variant={allChecked ? 'primary' : 'disabled'}
              isLoading={isLoading}
              onClick={handleAgree}
            >
              동의하고 시작하기
            </Button>
          </S.ButtonWrapper>
        </S.Container>
      </Layout.Content>
    </Layout>
  );
};

export default TermsAgreementPage;
