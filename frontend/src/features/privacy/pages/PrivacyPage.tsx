import BackButton from '@/components/@common/BackButton/BackButton';
import Layout from '@/layouts/Layout';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import * as S from './PrivacyPage.styled';

const PrivacyPage = () => {
  const navigate = useReplaceNavigate();

  return (
    <Layout>
      <Layout.TopBar left={<BackButton onClick={() => navigate('/')} />} />
      <Layout.Content>
        <S.Container>
          <S.Title>개인정보 처리방침</S.Title>
          <S.UpdatedAt>시행일: 2026년 5월 1일</S.UpdatedAt>

          <S.Section>
            <S.Body>
              {`쫄(ZZOL, 이하 "서비스")을 운영하는 팀(이하 "운영팀")은 개인정보보호법 제30조에 따라 이용자의 개인정보를 보호하고 관련 고충을 원활하게 처리할 수 있도록 다음과 같이 개인정보 처리방침을 수립·공개합니다.`}
            </S.Body>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제1조 수집하는 개인정보 항목 및 수집 방법</S.SectionTitle>
            <S.Table>
              <thead>
                <tr>
                  <S.Th>수집 항목</S.Th>
                  <S.Th>수집 목적</S.Th>
                  <S.Th>보유 기간</S.Th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <S.Td>소셜 계정 식별자, 닉네임</S.Td>
                  <S.Td>소셜 로그인(카카오·구글·네이버) 및 방 호스트 식별</S.Td>
                  <S.Td>회원 탈퇴 시 즉시 파기</S.Td>
                </tr>
                <tr>
                  <S.Td>닉네임 (참가자 직접 입력)</S.Td>
                  <S.Td>게임 방 내 참가자 식별</S.Td>
                  <S.Td>세션 종료 시 즉시 파기</S.Td>
                </tr>
                <tr>
                  <S.Td>접속 IP, 브라우저 정보, 방문 일시</S.Td>
                  <S.Td>서비스 오류 추적 및 보안 사고 대응 (Sentry)</S.Td>
                  <S.Td>수집일로부터 90일</S.Td>
                </tr>
              </tbody>
            </S.Table>
            <S.Body>
              수집 방법: 소셜 로그인 OAuth 인증, 이용자 직접 입력, 자동 수집(Sentry SDK)
            </S.Body>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제2조 개인정보 처리 목적</S.SectionTitle>
            <S.Body>
              {`운영팀은 다음 목적 이외의 용도로 개인정보를 이용하지 않으며, 목적이 변경될 경우 사전 동의를 받겠습니다.

• 서비스 제공: 게임 방 생성·참가, 미니게임 진행, 룰렛 당첨 처리
• 본인 확인 및 부정 이용 방지
• 서비스 장애 감지 및 오류 분석`}
            </S.Body>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제3조 개인정보의 제3자 제공</S.SectionTitle>
            <S.Body>
              {`운영팀은 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다. 다만, 소셜 로그인 이용 시 각 제공사(카카오·구글·네이버)의 개인정보 처리방침이 함께 적용됩니다.`}
            </S.Body>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제4조 개인정보 처리 위탁</S.SectionTitle>
            <S.Table>
              <thead>
                <tr>
                  <S.Th>수탁업체</S.Th>
                  <S.Th>위탁 업무</S.Th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <S.Td>Amazon Web Services (AWS)</S.Td>
                  <S.Td>프론트엔드 정적 파일 호스팅 (S3, CloudFront)</S.Td>
                </tr>
                <tr>
                  <S.Td>Oracle Cloud Infrastructure</S.Td>
                  <S.Td>백엔드 서버 인프라</S.Td>
                </tr>
                <tr>
                  <S.Td>Sentry</S.Td>
                  <S.Td>오류 로그 수집 및 모니터링</S.Td>
                </tr>
              </tbody>
            </S.Table>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제5조 개인정보의 파기</S.SectionTitle>
            <S.Body>
              {`개인정보 보유 기간 종료 또는 처리 목적 달성 시 지체 없이 파기합니다.
• 전자 파일: 복구 불가능한 방법으로 영구 삭제
• 세션 데이터: 브라우저 세션 종료 시 자동 삭제`}
            </S.Body>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제6조 이용자의 권리</S.SectionTitle>
            <S.Body>
              {`이용자는 언제든지 다음 권리를 행사할 수 있습니다.
• 개인정보 열람, 정정·삭제, 처리정지 요청
• 전송요구권 (개인정보보호법 §35조의2, 2025.3.13 시행)
• 자동화된 결정 대응권 (개인정보보호법 §37조의2)

요청은 서비스 내 문의 기능 또는 아래 담당자 이메일로 접수하며, 10일 이내 처리합니다.`}
            </S.Body>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제7조 개인정보 보호책임자</S.SectionTitle>
            <S.Body>
              {`개인정보 처리에 관한 업무를 총괄하고 고충을 처리합니다.

담당 팀: 쫄(ZZOL) 운영팀
이메일: zzol.contact@gmail.com`}
            </S.Body>
          </S.Section>

          <S.Section>
            <S.SectionTitle>제8조 개인정보 처리방침 변경</S.SectionTitle>
            <S.Body>
              {`본 방침은 법령·정책 변경 시 서비스 내 공지사항을 통해 사전 안내 후 개정됩니다. 변경 이력은 서비스 공지 또는 본 페이지에서 확인할 수 있습니다.`}
            </S.Body>
          </S.Section>
        </S.Container>
      </Layout.Content>
    </Layout>
  );
};

export default PrivacyPage;
