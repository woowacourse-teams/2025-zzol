import type { Meta, StoryObj } from '@storybook/react-webpack5';
import GameActionButtonSkeleton from './GameActionButtonSkeleton';

const meta = {
  title: 'Composition/GameActionButtonSkeleton',
  component: GameActionButtonSkeleton,
  parameters: {
    layout: 'padded',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof GameActionButtonSkeleton>;

export default meta;

type Story = StoryObj<typeof GameActionButtonSkeleton>;

export const Default: Story = {
  render: () => <GameActionButtonSkeleton />,
  parameters: {
    docs: {
      description: {
        story: '미니게임 버튼 목록이 로딩 중일 때의 모습입니다.',
      },
    },
  },
};
