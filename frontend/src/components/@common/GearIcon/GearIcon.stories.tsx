import type { Meta, StoryObj } from '@storybook/react-webpack5';
import GearIcon from './GearIcon';

const meta: Meta<typeof GearIcon> = {
  title: 'Common/GearIcon',
  component: GearIcon,
  tags: ['autodocs'],
  argTypes: {
    size: { control: { type: 'number' } },
    stroke: { control: { type: 'text' } },
  },
  parameters: { layout: 'centered' },
};
export default meta;

type Story = StoryObj<typeof GearIcon>;

export const Default: Story = {};

export const CustomSize: Story = {
  args: { size: 32 },
};

export const CustomStroke: Story = {
  args: { stroke: 'currentColor', size: 24 },
};
