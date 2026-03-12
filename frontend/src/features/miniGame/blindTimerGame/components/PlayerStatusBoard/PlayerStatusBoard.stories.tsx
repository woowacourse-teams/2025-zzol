import type { Meta, StoryObj } from '@storybook/react-webpack5';
import PlayerStatusBoard from './PlayerStatusBoard';

const meta = {
  title: 'Features/MiniGame/BlindTimerGame/PlayerStatusBoard',
  component: PlayerStatusBoard,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof PlayerStatusBoard>;

export default meta;

type Story = StoryObj<typeof PlayerStatusBoard>;

export const AllPlaying: Story = {
  args: {
    myName: '엠제이',
    players: [
      { playerName: '엠제이', stopped: false, timedOut: false },
      { playerName: '꾹이', stopped: false, timedOut: false },
      { playerName: '루키', stopped: false, timedOut: false },
      { playerName: '한스', stopped: false, timedOut: false },
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

export const SomeStopped: Story = {
  args: {
    myName: '엠제이',
    players: [
      { playerName: '엠제이', stopped: true, timedOut: false },
      { playerName: '꾹이', stopped: true, timedOut: false },
      { playerName: '루키', stopped: false, timedOut: false },
      { playerName: '한스', stopped: false, timedOut: false },
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

export const WithTimeout: Story = {
  args: {
    myName: '엠제이',
    players: [
      { playerName: '엠제이', stopped: true, timedOut: false },
      { playerName: '꾹이', stopped: true, timedOut: false },
      { playerName: '루키', stopped: true, timedOut: true },
      { playerName: '한스', stopped: true, timedOut: true },
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
