// TODO: 점검 종료 후 삭제 예정 - 서비스 점검 안내 모달
import Headline3 from '@/components/@common/Headline3/Headline3';
import Layout from '@/layouts/Layout';
import * as S from './ServiceMaintenanceModal.styled';

// TODO: 점검 종료 후 삭제 예정
const ServiceMaintenanceModal = () => {
  // TODO: 점검 종료 후 삭제 예정
  const MAINTENANCE_PERIOD = '2025.12.03(수) ~ 2025.12.10(수)';

  return (
    <Layout padding="0px">
      <Layout.TopBar center={<Headline3>⚠️서비스 점검 안내⚠️</Headline3>} />
      <Layout.Content>
        <S.ContentContainer>
          <S.Description>
            안정적인 서비스를 제공하기 위해
            <br />
            현재 서비스 점검을 진행하고 있습니다.
            <br />
            점검 기간 동안 서비스 이용이 일시 중단 되오니 양해 부탁드립니다.
          </S.Description>
          <S.PeriodContainer>
            <S.PeriodLabel>점검 기간</S.PeriodLabel>
            <S.PeriodDate>{MAINTENANCE_PERIOD}</S.PeriodDate>
          </S.PeriodContainer>
        </S.ContentContainer>
      </Layout.Content>
    </Layout>
  );
};

export default ServiceMaintenanceModal;
