import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { colorList } from '@/constants/color';
import CircleIcon from './CircleIcon';
import CoffeeCharacter from '@/assets/coffee-character.svg';

const meta: Meta<typeof CircleIcon> = {
  title: 'Common/CircleIcon',
  component: CircleIcon,
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

type Story = StoryObj<typeof CircleIcon>;

export const Default: Story = {
  args: {
    color: colorList[0],
    imageUrl: CoffeeCharacter,
    iconAlt: 'coffee-icon',
  },
  decorators: [
    (Story) => (
      <div style={{ width: '300px', padding: '20px' }}>
        <Story />
      </div>
    ),
  ],
};

export const AllColors: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
      {colorList.map((color) => (
        <div key={color} style={{ textAlign: 'center' }}>
          <CircleIcon color={color} imageUrl={CoffeeCharacter} iconAlt="coffee-icon" />
          <div style={{ fontSize: '12px', marginTop: '8px', color: '#666' }}>{color}</div>
        </div>
      ))}
    </div>
  ),
};

export const DifferentIcons: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
      <CircleIcon color={colorList[0]} imageUrl={CoffeeCharacter} iconAlt="coffee" />
      <CircleIcon color={colorList[2]} imageUrl={CoffeeCharacter} iconAlt="coffee-character" />
    </div>
  ),
};
