import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Flip from './Flip';
import { colorList } from '@/constants/color';

const meta: Meta<typeof Flip> = {
  title: 'Common/Flip',
  component: Flip,
  tags: ['autodocs'],
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    flipped: {
      control: { type: 'boolean' },
      description: '플립 상태 (true: 뒷면, false: 앞면)',
    },
  },
  decorators: [
    (Story) => (
      <div style={{ width: '300px', height: '300px' }}>
        <Story />
      </div>
    ),
  ],
};

export default meta;
type Story = StoryObj<typeof Flip>;

export const Default: Story = {
  args: {
    flipped: false,
    initialView: (
      <div
        style={{
          width: '100%',
          height: '100%',
          backgroundColor: colorList[0],
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontSize: '24px',
          fontWeight: 'bold',
        }}
      >
        앞면
      </div>
    ),
    flippedView: (
      <div
        style={{
          width: '100%',
          height: '100%',
          backgroundColor: colorList[1],
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontSize: '24px',
          fontWeight: 'bold',
        }}
      >
        뒷면
      </div>
    ),
  },
};

export const Flipped: Story = {
  args: {
    ...Default.args,
    flipped: true,
  },
};
