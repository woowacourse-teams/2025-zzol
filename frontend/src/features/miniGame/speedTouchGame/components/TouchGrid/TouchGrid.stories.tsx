import type { Meta, StoryObj } from '@storybook/react-webpack5';
import TouchGrid from './TouchGrid';

const meta = {
  title: 'Features/MiniGame/SpeedTouchGame/TouchGrid',
  component: TouchGrid,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof TouchGrid>;

export default meta;

type Story = StoryObj<typeof TouchGrid>;

const SHUFFLED_NUMBERS = [
  3, 17, 8, 24, 1, 12, 20, 5, 15, 9, 22, 2, 14, 7, 19, 25, 11, 4, 16, 23, 6, 18, 10, 21, 13,
];

export const GameStart: Story = {
  args: {
    numbers: SHUFFLED_NUMBERS,
    nextNumber: 1,
    isFinished: false,
    onTouch: () => {},
  },
  decorators: [
    (Story) => (
      <div style={{ width: '360px' }}>
        <Story />
      </div>
    ),
  ],
};

export const MidGame: Story = {
  args: {
    numbers: SHUFFLED_NUMBERS,
    nextNumber: 13,
    isFinished: false,
    onTouch: () => {},
  },
  decorators: [
    (Story) => (
      <div style={{ width: '360px' }}>
        <Story />
      </div>
    ),
  ],
};

export const NearFinish: Story = {
  args: {
    numbers: SHUFFLED_NUMBERS,
    nextNumber: 24,
    isFinished: false,
    onTouch: () => {},
  },
  decorators: [
    (Story) => (
      <div style={{ width: '360px' }}>
        <Story />
      </div>
    ),
  ],
};

export const Finished: Story = {
  args: {
    numbers: SHUFFLED_NUMBERS,
    nextNumber: 26,
    isFinished: true,
    onTouch: () => {},
  },
  decorators: [
    (Story) => (
      <div style={{ width: '360px' }}>
        <Story />
      </div>
    ),
  ],
};
