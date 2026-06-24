import { ReactNode } from 'react';
import * as S from './IconTextItem.styled';
import Paragraph from '../Paragraph/Paragraph';

type Props = {
  iconContent: ReactNode;
  textContent: ReactNode;
  rightContent?: ReactNode;
  gap?: number;
  showBorder?: boolean;
  onClick?: () => void;
};

const IconTextItem = ({
  iconContent,
  textContent,
  rightContent,
  gap = 20,
  showBorder = false,
}: Props) => {
  return (
    <S.Container $showBorder={showBorder}>
      <S.Wrapper $gap={gap}>
        <S.IconWrapper>{iconContent}</S.IconWrapper>
        <S.TextWrapper>
          <Paragraph>{textContent}</Paragraph>
        </S.TextWrapper>
      </S.Wrapper>
      {rightContent}
    </S.Container>
  );
};

export default IconTextItem;
