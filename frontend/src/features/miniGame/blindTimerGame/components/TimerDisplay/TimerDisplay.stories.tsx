import type { Meta, StoryObj } from '@storybook/react-webpack5';
import TimerDisplay from './TimerDisplay';

const meta = {
  title: 'Features/MiniGame/BlindTimerGame/TimerDisplay',
  component: TimerDisplay,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof TimerDisplay>;

export default meta;

type Story = StoryObj<typeof TimerDisplay>;

export const Counting: Story = {
  args: {
    displayTime: '2.45',
    isBlind: false,
    isStopped: false,
    stoppedTimeDisplay: null,
    onStop: () => {},
  },
};

export const Blind: Story = {
  args: {
    displayTime: '??.??',
    isBlind: true,
    isStopped: false,
    stoppedTimeDisplay: null,
    onStop: () => {},
  },
};

export const Stopped: Story = {
  args: {
    displayTime: '??.??',
    isBlind: true,
    isStopped: true,
    stoppedTimeDisplay: '11.23',
    onStop: () => {},
  },
};
