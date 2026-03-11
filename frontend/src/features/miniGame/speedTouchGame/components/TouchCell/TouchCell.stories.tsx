import type { Meta, StoryObj } from '@storybook/react-webpack5';
import TouchCell from './TouchCell';

const meta = {
  title: 'Features/MiniGame/SpeedTouchGame/TouchCell',
  component: TouchCell,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    onTouch: { action: 'touched' },
  },
} satisfies Meta<typeof TouchCell>;

export default meta;

type Story = StoryObj<typeof TouchCell>;

export const Untouched: Story = {
  args: {
    number: 7,
    touched: false,
    onTouch: () => {},
  },
  decorators: [
    (Story) => (
      <div style={{ width: '64px', height: '64px' }}>
        <Story />
      </div>
    ),
  ],
};

export const Touched: Story = {
  args: {
    number: 5,
    touched: true,
    onTouch: () => {},
  },
  decorators: [
    (Story) => (
      <div style={{ width: '64px', height: '64px' }}>
        <Story />
      </div>
    ),
  ],
};

export const Grid: Story = {
  render: () => {
    const numbers = [
      3, 17, 8, 24, 1, 12, 20, 5, 15, 9, 22, 2, 14, 7, 19, 25, 11, 4, 16, 23, 6, 18, 10, 21, 13,
    ];
    const nextNumber = 6;

    return (
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 64px)',
          gap: '6px',
        }}
      >
        {numbers.map((num) => (
          <TouchCell key={num} number={num} touched={num < nextNumber} onTouch={() => {}} />
        ))}
      </div>
    );
  },
};
