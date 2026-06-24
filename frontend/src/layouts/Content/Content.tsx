import { PropsWithChildren } from 'react';
import * as S from './Content.styled';

const Content = ({ children }: PropsWithChildren) => {
  return <S.Container>{children}</S.Container>;
};

export default Content;
