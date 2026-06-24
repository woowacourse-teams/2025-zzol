import type { Meta, StoryObj } from '@storybook/react-webpack5';
import GamePlayCountSlide from './GamePlayCountSlide';

const meta: Meta<typeof GamePlayCountSlide> = {
  title: 'Features/Home/DashBoard/GamePlayCountSlide',
  component: GamePlayCountSlide,
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
          width: '375px',
          height: '400px',
        }}
      >
        <Story />
      </div>
    ),
  ],
  argTypes: {
    games: {
      control: 'object',
      description: '미니게임 통계 데이터',
    },
  },
  args: {
    games: [
      { gameType: '카드게임', playCount: 20 },
      { gameType: '레이싱게임', playCount: 15 },
    ],
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const SingleGame: Story = {
  args: {
    games: [{ gameType: '카드게임', playCount: 20 }],
  },
};

export const MultipleGames: Story = {
  args: {
    games: [
      { gameType: '카드게임', playCount: 20 },
      { gameType: '레이싱게임', playCount: 15 },
      { gameType: '룰렛게임', playCount: 10 },
    ],
  },
};
