import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Divider from './Divider';

const meta: Meta<typeof Divider> = {
  title: 'Common/Divider',
  component: Divider,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    color: {
      control: 'color',
      description: '구분선 색상',
    },
    height: {
      control: 'text',
      description: '구분선 높이',
    },

    width: {
      control: 'text',
      description: '구분선 너비',
    },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: (args) => (
    <div
      style={{
        width: 300,
        height: 40,
        background: '#ffffff',
        display: 'flex',
        alignItems: 'center',
      }}
    >
      <Divider {...args} />
    </div>
  ),
  args: {},
};

export const CustomHeight: Story = {
  render: (args) => (
    <div
      style={{
        width: 300,
        height: 40,
        background: '#ffffff',
        display: 'flex',
        alignItems: 'center',
      }}
    >
      <Divider {...args} />
    </div>
  ),
  args: {
    height: '4px',
  },
};
