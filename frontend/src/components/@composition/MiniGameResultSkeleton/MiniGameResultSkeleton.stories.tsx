import type { Meta, StoryObj } from '@storybook/react-webpack5';
import MiniGameResultSkeleton from './MiniGameResultSkeleton';

const meta = {
  title: 'Components/Composition/MiniGameResultSkeleton',
  component: MiniGameResultSkeleton,
  parameters: {
    layout: 'padded',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof MiniGameResultSkeleton>;

export default meta;

type Story = StoryObj<typeof MiniGameResultSkeleton>;

export const Default: Story = {
  render: () => (
    <div style={{ maxWidth: '500px' }}>
      <MiniGameResultSkeleton />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '미니게임 결과 목록이 로딩 중일 때의 모습입니다.',
      },
    },
  },
};
