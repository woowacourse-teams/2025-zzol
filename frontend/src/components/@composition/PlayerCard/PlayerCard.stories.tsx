import Headline4 from '@/components/@common/Headline4/Headline4';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import PlayerCard from './PlayerCard';
import CoffeeCharacter from '@/assets/coffee-character.svg';
import { colorList } from '@/constants/color';

const meta = {
  title: 'Composition/PlayerCard',
  component: PlayerCard,
  tags: ['autodocs'],
} satisfies Meta<typeof PlayerCard>;

export default meta;

type Story = StoryObj<typeof PlayerCard>;

export const WithText: Story = {
  args: {
    name: '홍길동',
    playerColor: colorList[5],
    children: <Headline4>10%</Headline4>,
  },
};

export const WithIcon: Story = {
  args: {
    name: '김철수',
    playerColor: colorList[6],
    children: <img src={CoffeeCharacter} alt="coffee-character" />,
  },
};

export const Host: Story = {
  args: {
    name: '홍길동',
    playerColor: colorList[0],
    playerType: 'HOST',
  },
};

export const LongNameWithText: Story = {
  args: {
    name: '매우매우매우매우긴이름을가진플레이어',
    playerColor: colorList[1],
    children: <Headline4>15%</Headline4>,
  },
};

export const LongNameWithIcon: Story = {
  args: {
    name: '아주아주아주아주아주긴이름의사용자님',
    playerColor: colorList[2],
    children: <img src={CoffeeCharacter} alt="coffee-character" />,
  },
};

export const MultipleCards: Story = {
  render: () => (
    <>
      {Array.from({ length: 6 }, (_, index) => (
        <PlayerCard key={index} name="이영희" playerColor={colorList[3]}>
          <Headline4>20점</Headline4>
        </PlayerCard>
      ))}
    </>
  ),
};

export const DifferentProfileIcons: Story = {
  render: () => (
    <>
      <PlayerCard name="빨간색" playerColor={colorList[0]}>
        <Headline4>25%</Headline4>
      </PlayerCard>
      <PlayerCard name="파란색" playerColor={colorList[5]}>
        <Headline4>30%</Headline4>
      </PlayerCard>
      <PlayerCard name="초록색" playerColor={colorList[6]}>
        <Headline4>15%</Headline4>
      </PlayerCard>
    </>
  ),
};

export const Ready: Story = {
  args: {
    name: '홍길동',
    playerColor: colorList[5],
    isReady: true,
    children: <img src={CoffeeCharacter} alt="coffee-character" />,
  },
};
