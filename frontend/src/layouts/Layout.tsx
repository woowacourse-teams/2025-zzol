import { PropsWithChildren } from 'react';
import TopBar from './TopBar/TopBar';
import Banner from './Banner/Banner';
import Content from './Content/Content';
import ButtonBar from './ButtonBar/ButtonBar';
import { COLOR_MAP, ColorKey } from '@/constants/color';
import * as S from './Layout.styled';
import { LAYOUT_PADDING } from '@/constants/padding';

type LayoutProps = {
  color?: ColorKey;
  padding?: string;
} & PropsWithChildren;

const Layout = ({ color = 'white', padding = LAYOUT_PADDING, children }: LayoutProps) => (
  <S.LayoutContainer $color={COLOR_MAP[color]} $padding={padding}>
    {children}
  </S.LayoutContainer>
);

Layout.TopBar = TopBar;
Layout.Banner = Banner;
Layout.Content = Content;
Layout.ButtonBar = ButtonBar;

export default Layout;
