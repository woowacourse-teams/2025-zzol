import type { Meta, StoryObj } from '@storybook/react-webpack5';
import CardBack from './CardBack';

const meta = {
  title: 'Features/MiniGame/CardGame/CardBack',
  component: CardBack,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof CardBack>;

export default meta;

type Story = StoryObj<typeof CardBack>;

export const Small: Story = {
  args: {
    size: 'small',
  },
};

export const Medium: Story = {
  args: {
    size: 'medium',
  },
};

export const Large: Story = {
  args: {
    size: 'large',
  },
};

export const Disabled: Story = {
  args: {
    disabled: true,
  },
};

export const Grid: Story = {
  render: () => {
    return (
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '10px',
          placeItems: 'center',
        }}
      >
        {Array.from({ length: 9 }, (_, index) => (
          <CardBack key={index} />
        ))}
      </div>
    );
  },
  parameters: {
    layout: 'centered',
  },
};
