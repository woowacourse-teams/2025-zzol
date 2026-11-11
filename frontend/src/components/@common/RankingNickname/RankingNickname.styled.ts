import styled from '@emotion/styled';
import { srOnly } from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly.styled';

// gray-500은 흰 배경 대비 4.8:1로 AA를 넘는 가장 연한 회색이다.
export const Pending = styled.span`
  color: ${({ theme }) => theme.color.gray[500]};
  font-weight: 500;
`;

export const VisuallyHidden = styled.span`
  ${srOnly}
`;
