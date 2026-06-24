import { type ComponentProps } from 'react';

import NextStepIcon from '@/assets/next-step-icon.svg';
import { useButtonInteraction } from '@/hooks/useButtonInteraction';

import Description from '../Description/Description';
import Headline3 from '../Headline3/Headline3';

import * as S from './RoomActionButton.styled';
import ScreenReaderOnly from '../ScreenReaderOnly/ScreenReaderOnly';

type Props = {
  title: string;
  descriptions: string[];
  onClick?: () => void;
} & Omit<ComponentProps<'button'>, 'onClick'>;

const RoomActionButton = ({ title, descriptions, onClick, ...rest }: Props) => {
  const { touchState, pointerHandlers } = useButtonInteraction({ onClick });
  const testId = title === '방 만들기' ? 'create-room-button' : 'join-room-button';

  return (
    <S.Container {...pointerHandlers} $touchState={touchState} data-testid={testId} {...rest}>
      <Headline3>{title}</Headline3>
      <div>
        <ScreenReaderOnly>,</ScreenReaderOnly>
        {descriptions.map((description, index) => (
          <S.DescriptionBox key={index}>
            <Description color="gray-400">{description}</Description>
          </S.DescriptionBox>
        ))}
      </div>
      <S.NextStepIcon src={NextStepIcon} alt="next-step-icon" aria-hidden="true" />
    </S.Container>
  );
};

export default RoomActionButton;
