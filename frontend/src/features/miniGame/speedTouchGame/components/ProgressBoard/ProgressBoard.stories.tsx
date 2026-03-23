import type { Meta, StoryObj } from '@storybook/react-webpack5';
import ProgressBoard from './ProgressBoard';

const meta = {
  title: 'Features/MiniGame/SpeedTouchGame/ProgressBoard',
  component: ProgressBoard,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof ProgressBoard>;

export default meta;

type Story = StoryObj<typeof ProgressBoard>;

export const InProgress: Story = {
  args: {
    myName: '엠제이',
    players: [
      { playerName: '엠제이', currentNumber: 15, finished: false },
      { playerName: '꾹이', currentNumber: 20, finished: false },
      { playerName: '루키', currentNumber: 8, finished: false },
      { playerName: '한스', currentNumber: 12, finished: false },
    ],
  },
  decorators: [
    (Story) => (
      <div style={{ width: '360px' }}>
        <Story />
      </div>
    ),
  ],
};

export const SomeFinished: Story = {
  args: {
    myName: '엠제이',
    players: [
      { playerName: '엠제이', currentNumber: 26, finished: true },
      { playerName: '꾹이', currentNumber: 26, finished: true },
      { playerName: '루키', currentNumber: 18, finished: false },
      { playerName: '한스', currentNumber: 10, finished: false },
    ],
  },
  decorators: [
    (Story) => (
      <div style={{ width: '360px' }}>
        <Story />
      </div>
    ),
  ],
};

export const AllFinished: Story = {
  args: {
    myName: '엠제이',
    players: [
      { playerName: '엠제이', currentNumber: 26, finished: true },
      { playerName: '꾹이', currentNumber: 26, finished: true },
      { playerName: '루키', currentNumber: 26, finished: true },
      { playerName: '한스', currentNumber: 26, finished: true },
    ],
  },
  decorators: [
    (Story) => (
      <div style={{ width: '360px' }}>
        <Story />
      </div>
    ),
  ],
};
