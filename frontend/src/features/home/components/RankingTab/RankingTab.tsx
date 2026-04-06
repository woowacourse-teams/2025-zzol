import { RANKING_CATEGORIES } from '../../config/rankingConfigs';
import RankingAccordionItem from './RankingAccordionItem';
import * as S from './RankingTab.styled';

const RankingTab = () => {
  return (
    <S.Container>
      {RANKING_CATEGORIES.map((category) => (
        <RankingAccordionItem key={category.key} category={category} />
      ))}
    </S.Container>
  );
};

export default RankingTab;
