import type { Meta, StoryObj } from '@storybook/react-webpack5';
import TargetTime from './TargetTime';

const meta = {
  title: 'Features/MiniGame/BlindTimerGame/TargetTime',
  component: TargetTime,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof TargetTime>;

export default meta;

type Story = StoryObj<typeof TargetTime>;

export const Short: Story = {
  args: {
    targetTimeMillis: 5000,
  },
};

export const Medium: Story = {
  args: {
    targetTimeMillis: 11450,
  },
};

export const Long: Story = {
  args: {
    targetTimeMillis: 19990,
  },
};
