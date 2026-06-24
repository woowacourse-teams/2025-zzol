import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { colorList } from '@/constants/color';
import PlayerIcon from './PlayerIcon';

const meta: Meta<typeof PlayerIcon> = {
  title: 'Composition/PlayerIcon',
  component: PlayerIcon,
  tags: ['autodocs'],
  argTypes: {
    color: {
      control: { type: 'select' },
      options: colorList,
    },
  },
  parameters: {
    layout: 'centered',
  },
};
export default meta;

type Story = StoryObj<typeof PlayerIcon>;

export const Default: Story = {
  args: {
    color: colorList[0], // Use first color from colorList
  },
};

export const AllColors: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
      {colorList.map((color) => (
        <div key={color} style={{ textAlign: 'center' }}>
          <PlayerIcon color={color} />
          <div style={{ fontSize: '12px', marginTop: '8px', color: '#666' }}>{color}</div>
        </div>
      ))}
    </div>
  ),
};

// Generate individual stories for each color dynamically
export const Red: Story = {
  args: {
    color: colorList[0], // #FF6B6B
  },
};

export const Teal: Story = {
  args: {
    color: colorList[1], // #80d6d0
  },
};

export const Blue: Story = {
  args: {
    color: colorList[2], // #45B7D1
  },
};

export const Green: Story = {
  args: {
    color: colorList[3], // #96CEB4
  },
};

export const Yellow: Story = {
  args: {
    color: colorList[4], // #FFEAA7
  },
};

export const Purple: Story = {
  args: {
    color: colorList[5], // #DDA0DD
  },
};

export const Mint: Story = {
  args: {
    color: colorList[6], // #98D8C8
  },
};

export const Gold: Story = {
  args: {
    color: colorList[7], // #F7DC6F
  },
};

export const Lavender: Story = {
  args: {
    color: colorList[8], // #BB8FCE
  },
};
