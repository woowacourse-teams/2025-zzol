import type { Meta, StoryObj } from '@storybook/react-webpack5';
import CardFront from './CardFront';
import CardBack from '../CardBack/CardBack';
import { ColorList } from '@/constants/color';

const meta = {
  title: 'Features/MiniGame/CardGame/CardFront',
  component: CardFront,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof CardFront>;

export default meta;

type Story = StoryObj<typeof CardFront>;

export const Small: Story = {
  args: {
    size: 'small',
    card: { type: 'ADDITION', value: 10 },
  },
};

export const Medium: Story = {
  args: {
    size: 'medium',
    card: { type: 'MULTIPLIER', value: -1 },
  },
};

export const Large: Story = {
  args: {
    size: 'large',
    card: { type: 'ADDITION', value: 0 },
  },
};

export const WithPlayerSmall: Story = {
  args: {
    size: 'small',
    playerColor: '#FF6B6B',
    card: { type: 'MULTIPLIER', value: 0 },
  },
};

export const WithPlayerMedium: Story = {
  args: {
    size: 'medium',
    playerColor: '#FF6B6B',
    card: { type: 'MULTIPLIER', value: 2 },
  },
};

export const WithPlayerLarge: Story = {
  args: {
    size: 'large',
    playerColor: '#FF6B6B',
    card: { type: 'ADDITION', value: -40 },
  },
};

export const Grid: Story = {
  render: () => {
    const playerColorMap = [
      '#FF6B6B' as ColorList,
      '#4cafa9' as ColorList,
      '#85b62e' as ColorList,
      '#bf77f6' as ColorList,
      '#ffa102' as ColorList,
      '#5a88c8' as ColorList,
      '#ff8ad8' as ColorList,
      '#8a8b8e' as ColorList,
      '#1d6d4a' as ColorList,
    ];

    return (
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '10px',
          placeItems: 'center',
        }}
      >
        {Array.from({ length: 9 }, (_, index) =>
          playerColorMap[index] ? (
            <CardFront
              key={index}
              playerColor={playerColorMap[index]}
              card={{ type: 'MULTIPLIER', value: -1 }}
              isMyCard={index === 1}
              playerName={`Player ${index + 1}`}
            />
          ) : (
            <CardBack key={index} />
          )
        )}
      </div>
    );
  },
  parameters: {
    layout: 'centered',
  },
};
