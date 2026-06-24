import type { Meta, StoryObj } from '@storybook/react-webpack5';
import RankingItem from './RankingItem';

const meta: Meta<typeof RankingItem> = {
  title: 'Common/RankingItem',
  component: RankingItem,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <div
        style={{
          background: '#ff6b6b',
          padding: '2rem',
          borderRadius: '20px',
          width: '300px',
        }}
      >
        <Story />
      </div>
    ),
  ],
  argTypes: {
    rank: {
      control: { type: 'range', min: 1, max: 10, step: 1 },
      description: '랭킹 순위',
    },
    name: {
      control: 'text',
      description: '플레이어 이름',
    },
    count: {
      control: { type: 'range', min: 1, max: 100, step: 1 },
      description: '횟수',
    },
  },
  args: {
    rank: 1,
    name: '세라',
    count: 20,
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const MultipleRanks: Story = {
  render: () => (
    <div>
      <RankingItem rank={1} name="세라" count={20} />
      <RankingItem rank={2} name="민수" count={15} />
      <RankingItem rank={3} name="지영" count={12} />
    </div>
  ),
  parameters: {
    controls: { disable: true },
  },
};
