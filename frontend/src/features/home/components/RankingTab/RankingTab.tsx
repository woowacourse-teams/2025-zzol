import DashBoard from '../DashBoard/DashBoard';
import { RANKING_CATEGORIES } from '../../config/rankingConfigs';
import RankingAccordionItem from './RankingAccordionItem';
import * as S from './RankingTab.styled';

const RankingTab = () => {
  return (
    <S.Container>
      <S.StatsSection>
        <S.StatsSectionTitle>이달의 통계</S.StatsSectionTitle>
        <DashBoard />
      </S.StatsSection>

      <S.Divider />

      <S.RankingSection>
        <S.StatsSectionTitle>전체 랭킹</S.StatsSectionTitle>
        <S.AccordionCard>
          {RANKING_CATEGORIES.map((category) => (
            <RankingAccordionItem key={category.key} category={category} />
          ))}
        </S.AccordionCard>
      </S.RankingSection>
    </S.Container>
  );
};

export default RankingTab;
